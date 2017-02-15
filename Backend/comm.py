import socket
from thread import *
import re


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
    regex = re.compile(r'(.*),(.*),(\d+),([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+[-+]?|\d+),([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+)')

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

def receive_msg(conn):
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

        matched = matched.group(0)
        print(str(matched) + " Matched.")
        if matched == "SEND":
            # SEND message
            # get length of data
            matched = re.match("SEND (\d+)\n", chunk.decode('ascii')[0:])
            print("Header length: " + str(len(matched.group(0))) + " Payload length:" + matched.group(1))
            length = len(matched.group(0)) + int(matched.group(1))  # Header len + payload len

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
            result = store_msg_to_DB(data)
            if result:
                send_OK(conn)
            else:
                send_ERROR(conn, "Something bad in the CSV format")
            pass
        else:
            # Close connection otherwise
            send_ERROR(conn, "Bad message. You should send a SEND message")
    # End connection
    conn.close()

if __name__ == "__main__":
    # create an INET, STREAMing socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # bind the socket to a public host, and a well-known port
    sock.bind(('', 9999))
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
