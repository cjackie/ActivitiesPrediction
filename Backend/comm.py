import socket
from _thread import *

def receive_msg(conn):
    send_data = bytearray("CONNECTED\n".encode())
    try:
        conn.sendall(send_data)
        print("Sent to client: " + str(send_data))
    except Exception as e:
        print("Send error: " + str(e))

    while True:
        data = conn.recv(1024)
        if not data:    # On close, it gets EOF. In python EOF is empty string
            break
        print("RECEIVED: " + str(data))
        try:
            send_data = bytearray("RECEIVED: ".encode())
            send_data.extend(data)
            conn.sendall(send_data)
            print("Sent to client: " + str(send_data))
        except Exception as e:
            print("Send error: " + str(e))
            break
    # End connection
    conn.close()

if __name__ == "__main__":
    # create an INET, STREAMing socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # bind the socket to a public host, and a well-known port
    sock.bind(('', 9999))
    # become a server socket
    sock.listen(5)

    while True:
        conn, address = sock.accept()
        print("Socket accepted: " + str(address))
        start_new_thread(receive_msg, (conn, ))
        pass
    # Unreachable
    sock.close()
