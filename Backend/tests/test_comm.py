import socket

def test_predict(sock):
    try:
        nums = '\n'.join(['408010312481149.000000,-0.703895,5.190629,7.556100',
                '408010392583597.000000,-0.871489,5.822699,7.278373',
                '408010432617191.000000,-0.536301,5.631162,6.181829',
                '408010492658389.000000,-0.833182,6.330269,5.449203',
                '408010552901514.000000,-1.723825,6.224925,6.315904',
                '408010692910837.000000,-1.422156,5.650316,7.431602',
                '408010732553597.000000,-1.197101,5.674258,7.091625',
                '408010812632139.000000,-1.465251,5.554548,6.991069',
                '408010873354482.000000,-1.824382,5.918467,6.584054',
                '408010893088336.000000,-1.733402,5.980716,6.397307',
                '408010953326305.000000,-1.584962,5.990293,6.397307',
                '408010993368961.000000,-1.470040,5.770026,6.320693',
                '408011093343076.000000,-1.881842,5.755661,7.058106',
                '408011173038805.000000,-1.675941,5.803545,6.828263',
                '408011233643545.000000,-1.680730,5.861006,6.698977',
                '408011353727711.000000,-1.383849,5.937620,6.521805',
                '408011433867451.000000,-1.771709,5.817910,6.708553',
                '408011493740524.000000,-1.781286,5.956774,6.670246',
                '408011533649638.000000,-1.699883,5.803545,6.722919',
                '408011573668805.000000,-1.709460,6.023811,6.713342',
                '408011653737347.000000,-1.254562,5.698200,6.895301',
                '408011714367190.000000,-1.446098,5.396531,6.411672',
                '408011773803024.000000,-1.589750,5.616797,7.053318',
                '408011834310107.000000,-1.450886,5.683835,6.828263',
                '408011894334742.000000,-1.786075,5.544971,7.086837',
                '408011933866513.000000,-1.642422,5.621585,7.173028',
                '408012094413753.000000,-1.484405,5.439626,6.962338',
                '408012154703284.000000,-1.527501,5.549759,6.856994',
                '408012234571409.000000,-1.766921,5.430049,7.000646',
                '408012294609638.000000,-1.661576,5.674258,6.823475',
                '408012334378909.000000,-1.589750,5.669470,6.933608',
                '408012414491096.000000,-1.690306,5.698200,6.761226',
                '408012474846096.000000,-1.987187,5.956774,6.722919',
                '408012494838544.000000,-1.843535,6.038177,6.627151',
                '408012894838544.000000,-3.843535,4.038177,2.627151'])
        data_sent = 'PREDICT {0}\n'.format(len(nums)) + nums
        sock.sendall(data_sent.encode('ascii'))

        recv_data = sock.recv(1024)
        print("Received: " + str(recv_data))
    except Exception as e:
        print("Sending Error: " + str(e))
        # Receive
    finally:
        sock.close()


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

        # print("testing predict")
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(("0.0.0.0", 8000))
        test_predict(sock)
    elif len(sys.argv) is 2:
        if (sys.argv[1] == "--help") or (sys.argv[1] == "-h"):
            print("Usage: python connection_test.py [CSV dataset file path]")
            print("\t\tProtocol test script. Send a CSV dataset file to the server.")
            print("\t\tIf there is no argument, it goes a simple protocol test mode.")
            exit(0)
        print("Dataset transmission mode")
        send_dataset(sock, sys.argv[1])
    exit(0)




