import socket

def simple_test(sock):
    while True:
        # Send
        try:
            send_data = raw_input("Send any data ...")
            sock.sendall(send_data.encode())
        except Exception as e:
            print("Sending Error: " + str(e))
        # Receive
        recv_data = sock.recv(1024)
        if not recv_data:
            print("Connection closed by the server")
            break
        print("Received: " + str(recv_data))

    sock.close()

def send_dataset(sock, filename):
    # Open file.
    print("Opening file..")

    f = None
    try:
        f = open(filename, 'rb')
    except Exception as e:
        print("Failed opening the file: " + str(e))
        sock.close()
        exit(1)

    # Then send bytes to the server.
    try:
        data = f.read()
        send_data = bytearray("SEND {}\n".format(len(data)).encode())
        send_data.extend(data)
        print("Sending data {} bytes...".format(len(send_data)))
        sock.sendall(send_data)
    except Exception as e:
        print("Sending Error: " + str(e))
    recv_data = sock.recv(1024)
    if not recv_data:
        print("Connection closed by the server")
    print("Received: " + str(recv_data))

    # close socket and file
    sock.close()
    f.close()

if __name__ == "__main__":
    # create an INET, STREAMing socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # now connect to the web server on port 80 - the normal http port
    sock.connect(("0.0.0.0", 8000))

    import sys
    if len(sys.argv) is 1:
        print("Simple protocol test mode")
        simple_test(sock)
    elif len(sys.argv) is 2:
        if (sys.argv[1] == "--help") or (sys.argv[1] == "-h"):
            print("Usage: python connection_test.py [CSV dataset file path]")
            print("\t\tProtocol test script. Send a CSV dataset file to the server.")
            print("\t\tIf there is no argument, it goes a simple protocol test mode.")
            exit(0)
        print("Dataset transmission mode")
        send_dataset(sock, sys.argv[1])
    exit(0)




