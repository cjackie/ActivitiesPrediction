import socket
from thread import *
from threading import Lock
import re
import os
import sys
from model import get_default_model
import numpy as np


'''
depending on data/local_data directory to work.
create a empty directory to start
'''

FILE_NUM_PATH = os.path.join(os.path.dirname(__file__), 'data/local_data/file_num')
LABELS_FILE_PATH = os.path.join(os.path.dirname(__file__), 'data/local_data/labels')
# let it be x. it means for every x data file created, the model is rebuilt.
REBUILD_MODEL_MAGIC_NUM = 5


file_num = None
labels_file = None
local_file_creation_lock = Lock()

my_model_lock = Lock()
file_num_for_model = None
my_model = None

verbose = True


def init():
    '''
    init global variables
    '''
    global file_num
    global labels_file
    if not os.path.isfile(FILE_NUM_PATH):
        f = open(FILE_NUM_PATH, 'w')
        f.write('0')
        f.close()
        file_num = 0
        print("initialized local_data directory")
    else:
        f = open(FILE_NUM_PATH, 'r')
        file_num = int(f.read())
        f.close()

    if not os.path.isfile(LABELS_FILE_PATH):
        labels_file = open(LABELS_FILE_PATH, 'w')
    else:
        labels_file = open(LABELS_FILE_PATH, 'a')

    global my_model
    global file_num_for_model
    my_model = get_default_model(verbose)
    file_num_for_model = file_num

    def _exit():
        global labels_file
        global file_num

        labels_file.close()
        with open(FILE_NUM_PATH, 'w') as f:
            f.write(str(file_num))

    sys.exitfunc = _exit

def flush_labels_and_file_num():
    global labels_file
    global file_num

    labels_file.flush()
    os.fsync(labels_file.fileno())
    with open(FILE_NUM_PATH, 'w') as f:
        f.write(str(file_num))


def invalid_msg(data):
    print("Invalid message received: " + str(data))
    print("Close socket")


def send_OK(conn):
    send_data = bytearray("OK\n".encode())
    try:
        conn.sendall(send_data)
    except Exception as e:
        print("Send error: " + str(e))
    pass


def send_LABEL(conn, label):
    send_data = bytearray('LABEL {0}'.format(label).encode())
    try:
        conn.sendall(send_data)
    except Exception as e:
        print("Send error: " + str(e))
    pass


def send_ERROR(conn, message):
    message = message.encode()
    send_data = bytearray("ERROR {}\n".format(len(message)).encode())
    send_data.extend(bytearray(message))
    try:
        conn.sendall(send_data)
    except Exception as e:
        print("Send error: " + str(e))
    pass


def maybe_rebuild_model():
    '''
    FIXME updated model seems not visible to new requests(threads)
    :return:
    '''
    global file_num_for_model
    global file_num
    global my_model
    global my_model_lock

    my_model_lock.acquire()
    if file_num != file_num_for_model and file_num % REBUILD_MODEL_MAGIC_NUM == 0:
        if verbose:
            print('rebuiding model at {0}'.format(file_num))
        flush_labels_and_file_num() # needed for get_default_model to take new data.
        my_model = get_default_model(verbose)
        file_num_for_model = file_num
    my_model_lock.release()


def store_msg_to_local(data, label):
    global local_file_creation_lock
    global labels_file
    global file_num

    local_file_creation_lock.acquire()
    cur_file_num = file_num
    file_num += 1
    accel_file_name = 'accel' + str(cur_file_num) + '.csv'
    labels_file.write('{0},{1}\n'.format(accel_file_name, label))
    local_file_creation_lock.release()

    accel_file = open(os.path.join(os.path.dirname(__file__), 'data/local_data/'+accel_file_name), "w")
    # time,Ax,Ay,Az
    regex = re.compile(r'([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+),'
                       r'([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+)')
    for m in regex.finditer(data):
        time = m.group(1)
        x = m.group(2)
        y = m.group(3)
        z = m.group(4)
        accel_file.write('{0},{1},{2},{3}\n'.format(time, x, y, z))
    accel_file.close()


def _read_raw(data_raw):
    '''
    :param data: String. just raw data of comma seperated values.
            time, x, y, z
    :return: numpy array. shape is (n, 4), or (0,)
    '''
    # time,Ax,Ay,Az
    data = []
    regex = re.compile(r'([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+),'
                       r'([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+)')
    for m in regex.finditer(data_raw):
        time = float(m.group(1))
        x = float(m.group(2))
        y = float(m.group(3))
        z = float(m.group(4))
        data.append([time, x, y, z])
    return np.array(data)


def predict(data):
    '''
    :param data: String. just raw data of comma seperated values.
            time, x, y, z
    :return: String. label
    '''
    data_np = _read_raw(data)
    if len(data_np) == 0:
        return 'NA'

    global my_model
    my_model_lock.acquire()
    try:
        # from nano sec to sec
        data_np[:,0] = data_np[:,0] / 1000000000
        return my_model.predict_with_time(data_np)
    except Exception as e:
        return 'NA'
    finally:
        my_model_lock.release()


def receive_msg(conn):
    '''
    Two header types are available, and they are in ascii.
    - SEND [length] [label]
        time,x,y,z
        time,x,y,z
    - PREDICT [length]
        time,x,y,z
        time,x,y,z
    length is the total data size.
    :exception: from sockets
    '''
    # Start Loop
    regex = re.compile('[A-Z]+')
    while True:
        chunks = []
        # Receive header first
        chunk = conn.recv(512)
        if chunk == '':
            print("Socket disconnected")
            break
        bytes_recd = len(chunk)
        print('{} bytes received').format(bytes_recd)
        chunks.append(chunk)
        # Get type of message
        matched = regex.match(chunk.decode('ascii'))
        # If message didn't find match, close the socket
        if not matched:
            invalid_msg(chunk)
            break

        # getting data
        matched = matched.group(0)

        print(str(matched) + " Matched.")
        if matched == "SEND":
            # get length of data
            matched = re.match("SEND (\d+) ([a-z|A-Z]+)", chunk.decode('ascii'))
            print("Header length: " + str(len(matched.group(0))) + " Payload length:" + matched.group(1) +
                  " Label: " + matched.group(2))
            length = len(matched.group(0)) + int(matched.group(1))  # Header len + payload len
            label = matched.group(2)

            # receive that amount of data
            while bytes_recd < length:
                chunk = conn.recv(min(length - bytes_recd, 2048))
                if chunk == '':
                    raise RuntimeError("socket connection broken")
                chunks.append(chunk)
                bytes_recd = bytes_recd + len(chunk)
                print('{} bytes received. Total: {}').format(len(chunk), bytes_recd)
            data = ''.join(chunks).decode('ascii')

            # Store data
            if verbose:
                print(str(data))
            store_msg_to_local(data, label)
            send_OK(conn)
            maybe_rebuild_model()
            break

        elif matched == 'PREDICT':
            matched = re.match("PREDICT (\d+)", chunk.decode('ascii')[0:])
            length = len(matched.group(0)) + int(matched.group(1))
            bytes_recd = len(chunk)

             # receive that amount of data
            while bytes_recd < length:
                chunk = conn.recv(min(length - bytes_recd, 2048))
                if chunk == '':
                    raise RuntimeError("socket connection broken")
                chunks.append(chunk)
                bytes_recd = bytes_recd + len(chunk)
                print('{} bytes received. Total: {}').format(len(chunk), bytes_recd)
            data = ''.join(chunks).decode('ascii')

            label = predict(data)
            send_LABEL(conn, label)
            break

        else:
            # Close connection otherwise
            send_ERROR(conn, "Bad message. You should send a SEND message")
            break


    # End connection
    conn.close()


if __name__ == "__main__":
    init()
    # create an INET, STREAMing socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # bind the socket to a public host, and a well-known port
    sock.bind(('', 8000))
    # become a server socket
    sock.listen(5)

    print("Server is running...\n")
    while True:
        conn, address = sock.accept()
        print("Socket accepted: " + str(address))
        start_new_thread(receive_msg, (conn, ))
        pass
    # Unreachable
    sock.close()
