import socket
from thread import *
from threading import Lock
import re
import os
import sys


'''
depending on data/local_data directory to work.
create a empty directory to start
'''

FILE_NUM_PATH = os.path.join(os.path.dirname(__file__), 'data/local_data/file_num')
LABELS_FILE_PATH = os.path.join(os.path.dirname(__file__), 'data/local_data/labels')


file_num = None
labels_file = None
local_file_creation_lock = Lock()


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

    def _exit():
        global labels_file
        global file_num

        labels_file.close()
        with open(FILE_NUM_PATH, 'w') as f:
            f.write(str(file_num))

    sys.exitfunc = _exit


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


def store_msg_to_DB(data):
    #  label ,pos,time,Ax,Ay,Az,Gx,Gy,Gz,Mx,My,Mz
    regex = re.compile(r'(.*),(.*),(\d+),'
        r'([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+),'
        r'([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+),'
        r'([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+)')

    # generate objects
    from db import Data
    objs = []
    keys = ('label', 'position', 'timestamp',
                'ax', 'ay', 'az',
                'gx', 'gy', 'gz',
                'mx', 'my', 'mz')
    for m in regex.finditer(data):
        i = 1
        args = {}
        for key in keys:
            args[key] = m.group(i)
            i += 1
        print(args)
        obj = Data(**args)
        objs.append(obj)

    # and then store to the DB
    for obj in objs:
        obj.insert()
    return True


def store_msg_to_local(data, label):
    global local_file_creation_lock
    global labels_file
    global file_num

    local_file_creation_lock.acquire()
    cur_file_num = file_num
    file_num += 1
    accel_file_name = 'data/local_data/accel' + str(cur_file_num) + '.csv'
    labels_file.write('{0},{1}\n'.format(accel_file_name, label))
    local_file_creation_lock.release()

    accel_file = open(os.path.join(os.path.dirname(__file__), accel_file_name), "w")
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


def predict(data):
    # TODO
    return 'NA'


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
    '''
    # Start Loop
    regex = re.compile('[A-Z]+')
    while True:
        chunks = []
        # Receive header first
        try:
            chunk = conn.recv(512)
        except socket.error as e:
            import errno
            if e.errno == errno.ECONNRESET:
                # Handle disconnection
                print("Socket is reset ungracefully by the client. Close socket.")
                break
            else:
                # Other error, re-raise
                print("Unknown error. Close socket.")
                break
        if not chunk:
            print("Socket disconnected")
            break
        bytes_recd = len(chunk)
        print('{} bytes received').format(bytes_recd)
        chunks.append(chunk)
        # Get type of message
        matched = regex.match(chunk.decode('ascii')[0:])
        # If message didn't find match, close the socket
        if not matched:
            invalid_msg(chunk)
            break

        # getting data
        matched = matched.group(0)

        print(str(matched) + " Matched.")
        if matched == "SEND" or matched == 'PREDICT':
            # get length of data
            matched = re.match("SEND (\d+) ([a-z|A-Z]+)", chunk.decode('ascii')[0:])
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
            data = ''.join(chunks)

            # Store data
            print(str(data))
            store_msg_to_local(data, label)
            send_OK(conn)
            break

        elif matched == 'PREDICT':
            matched = re.match("PREDICT (\d+)", chunk.decode('ascii')[0:])
            length = len(matched.group(0)) + int(matched.group(1))

             # receive that amount of data
            while bytes_recd < length:
                chunk = conn.recv(min(length - bytes_recd, 2048))
                if chunk == '':
                    raise RuntimeError("socket connection broken")
                chunks.append(chunk)
                bytes_recd = bytes_recd + len(chunk)
                print('{} bytes received. Total: {}').format(len(chunk), bytes_recd)
            data = ''.join(chunks)

            # TODO
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
