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


file_num = None
labels_file = None
local_file_creation_lock = Lock()

my_model_lock = Lock()
my_model = None


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
    my_model = get_default_model(True)

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
    accel_file_name = 'accel' + str(cur_file_num) + '.csv'
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

    # from nano sec to sec
    # data_np[:,0] = data_np[:,0] / 1000000000

     # from test model
    data = [[408010312481149.000000,-0.703895,5.190629,7.556100],
            [408010392583597.000000,-0.871489,5.822699,7.278373],
            [408010432617191.000000,-0.536301,5.631162,6.181829],
            [408010492658389.000000,-0.833182,6.330269,5.449203],
            [408010552901514.000000,-1.723825,6.224925,6.315904],
            [408010692910837.000000,-1.422156,5.650316,7.431602],
            [408010732553597.000000,-1.197101,5.674258,7.091625],
            [408010812632139.000000,-1.465251,5.554548,6.991069],
            [408010873354482.000000,-1.824382,5.918467,6.584054],
            [408010893088336.000000,-1.733402,5.980716,6.397307],
            [408010953326305.000000,-1.584962,5.990293,6.397307],
            [408010993368961.000000,-1.470040,5.770026,6.320693],
            [408011093343076.000000,-1.881842,5.755661,7.058106],
            [408011173038805.000000,-1.675941,5.803545,6.828263],
            [408011233643545.000000,-1.680730,5.861006,6.698977],
            [408011353727711.000000,-1.383849,5.937620,6.521805],
            [408011433867451.000000,-1.771709,5.817910,6.708553],
            [408011493740524.000000,-1.781286,5.956774,6.670246],
            [408011533649638.000000,-1.699883,5.803545,6.722919],
            [408011573668805.000000,-1.709460,6.023811,6.713342],
            [408011653737347.000000,-1.254562,5.698200,6.895301],
            [408011714367190.000000,-1.446098,5.396531,6.411672],
            [408011773803024.000000,-1.589750,5.616797,7.053318],
            [408011834310107.000000,-1.450886,5.683835,6.828263],
            [408011894334742.000000,-1.786075,5.544971,7.086837],
            [408011933866513.000000,-1.642422,5.621585,7.173028],
            [408012094413753.000000,-1.484405,5.439626,6.962338],
            [408012154703284.000000,-1.527501,5.549759,6.856994],
            [408012234571409.000000,-1.766921,5.430049,7.000646],
            [408012294609638.000000,-1.661576,5.674258,6.823475],
            [408012334378909.000000,-1.589750,5.669470,6.933608],
            [408012414491096.000000,-1.690306,5.698200,6.761226],
            [408012474846096.000000,-1.987187,5.956774,6.722919],
            [408012494838544.000000,-1.843535,6.038177,6.627151],
            [408012894838544.000000,-3.843535,4.038177,2.627151]]
    data = np.array(data)
    data[:,0] = data[:,0] / 1000000000
    data_np = data

    return my_model.predict_with_time(data_np)



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
        if matched == "SEND":
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
            bytes_recd = len(chunk)

             # receive that amount of data
            while bytes_recd < length:
                chunk = conn.recv(min(length - bytes_recd, 2048))
                if chunk == '':
                    raise RuntimeError("socket connection broken")
                chunks.append(chunk)
                bytes_recd = bytes_recd + len(chunk)
                print('{} bytes received. Total: {}').format(len(chunk), bytes_recd)
            data = ''.join(chunks)

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
