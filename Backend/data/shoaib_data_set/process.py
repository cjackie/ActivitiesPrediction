import numpy as np
import os


def data(seq_len, verbose = True):

    def get_wrist(tokens):
        time_i = 42
        accel_i = 43
        gyro_i = 49

        time = float(tokens[time_i])
        accel = map(lambda s: float(s), tokens[accel_i:accel_i+3])
        gyro = map(lambda s: float(s), tokens[gyro_i:gyro_i+3])
        return time, accel, gyro


    class SeqData():
        def __init__(self, seq_len):
            self.__seq_len = seq_len
            self.__batch_cap = 1
            self.__data = np.zeros((self.__batch_cap, 3, self.__seq_len, 1))
            self.__batch_i = 0
            self.__seq_i = 0

        def append(self, seq):
            '''
            @seq, list of number. len is 3
            '''
            assert len(seq) == 3
            self.__data[self.__batch_i,:,self.__seq_i,0] = seq

            # update index
            self.__seq_i += 1
            if self.__seq_i >= self.__seq_len:
                self.__batch_i += 1
                self.__seq_i = 0
            if self.__batch_i >= self.__batch_cap:
                # need to increase size
                new_batch_cap = self.__batch_cap*2
                new_data = np.zeros((new_batch_cap, 3, self.__seq_len, 1))
                new_data[0:self.__batch_cap,:,:,:] = self.__data
                self.__data = new_data
                self.__batch_cap = new_batch_cap

        def get_data(self):
            return self.__data[0:self.__batch_i,:,:,:]

    ### main ###
    accel_data_container = SeqData(seq_len)
    gyro_data_container = SeqData(seq_len)

    for i in xrange(1, 11):
        filename = os.path.join(os.path.dirname(__file__), 'Participant_{0}.csv'.format(str(i)))
        try:
            csv_file = open(filename, 'r')
            csv_file.readline()
            csv_file.readline()
        except Exception as e:
            if verbose:
                print('can\'t open ' + filename)
            continue

        raw_data = csv_file.read()
        lines = raw_data.split('\n')
        for line in lines:
            tokens = line.split(',')
            try:
                time, accel, gyro = get_wrist(tokens)
            except Exception as e:
                if verbose:
                    print('invalid format line')
                continue

            accel_data_container.append(accel)
            gyro_data_container.append(gyro)

    accel_data = accel_data_container.get_data()
    gyro_data = gyro_data_container.get_data()

    return accel_data, gyro_data

