import socket

if __name__ == "__main__":
    # create an INET, STREAMing socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # now connect to the web server on port 80 - the normal http port
    sock.connect(("www.kbumsik.net", 9999))

    # Send
    while True:
        try:
            send_data = raw_input("Send any data ...")
            sock.sendall(send_data.encode())
        except Exception as e:
            print("Sending Error: " + str(e))
        recv_data = sock.recv(1024)
        if not recv_data:
            print("Connection closed by the server")
            break
        print("Received: " + str(recv_data))

    sock.close()
