import numpy as np
import os

def get_data(data_raw, start_i, length):
    '''
    @data_raw: array. each element is a string encode comma seperated values.
       assume each element is x,y,z,time
    @start_i: int.
    @length: int.
    @return: numpy array. shape of (3, config['time_length'])
    '''
    data = np.ndarray(shape=(3, length))
    for i in range(length):
        line = data_raw[start_i+i]
        tokens = line.split(',')
        data[0,i] = float(tokens[0])
        data[1,i] = float(tokens[1])
        data[2,i] = float(tokens[2])
    return data

def get_data_from_files(filename_list, config):
    '''
    @filename_list: list of string. each file csv formatted of "x,y,z,time"
    @return: ndarray. shape (n, 3, config['time_length'], 1))
    '''
    data_is_empty = True
    for fn in filename_list:
        f = open(fn, 'r')
        f.readline()
        lines = f.read().strip().split('\n')
        f.close()

        number_seq = len(lines) / config['time_length']
        for i in range(number_seq):
            data_i = get_data(lines, i*config['time_length'], config['time_length'])
            data_i = np.expand_dims(np.expand_dims(data_i, 0), -1)
            if data_is_empty:
                data = data_i
                data_is_empty = False
            else:
                data = np.concatenate((data, data_i), axis=0)
    return data

def _interpolate(seq, period, tolerance=1.1, min_segment_len=2):
    '''
    @seq: np array of shape (n, 4). where seq[:,0], is time(unit second) in 
        increasing order, seq[:,1] is x, seq[:,2] is y, seq[:,3] is z. 
    @period: double. time between adjacent data point. unit second.
    @tolerance: double. how much time gap more than period is allowed. 
        period*(1+tolerance).

    @return: array of numpy array. valid interploated seqments.
        a numpy array has shape [?, 4]
    '''
    assert seq.shape[0] > 1

    # determine valid segments
    start_i = 0
    prev_time = seq[0,0]
    seq_segments = []
    for cur_i in range(1, seq.shape[0]):
        cur_time = seq[cur_i, 0]
        if cur_time - prev_time > period*(1+tolerance):
            # a segment is determined
            seq_segments.append(seq[start_i:cur_i, :])
            start_i = cur_i
        prev_time = cur_time
    # last one
    if start_i < cur_i:
        seq_segments.append(seq[start_i:cur_i, :])

    # interpolate
    seq_segments_interped = []
    for seq_seg in seq_segments:
        start_time = seq_seg[0,0]
        end_time = seq_seg[-1,0]
        num_of_points = int((end_time - start_time) / period)
        if num_of_points < min_segment_len:
            continue

        time_points = [start_time+period/2.0+i*period for i in range(num_of_points)]
        xs = np.interp(time_points, seq_seg[:,0], seq_seg[:,1])
        ys = np.interp(time_points, seq_seg[:,0], seq_seg[:,2])
        zs = np.interp(time_points, seq_seg[:,0], seq_seg[:,3])
        seq_seg_interped = np.stack((time_points, xs, ys, zs), axis=1)

        seq_segments_interped.append(seq_seg_interped)

    return seq_segments_interped

def _interpolate2(seq, period): 
    '''
    @seq: np array of shape (n, 4). where seq[:,0], is time(unit second) in 
        increasing order, seq[:,1] is x, seq[:,2] is y, seq[:,3] is z. 
    @period: double. time between adjacent data point. unit second.

    @return: numpy array. it has shape [?, 4]
    '''
    start_time = seq[0,0]
    end_time = seq[-1,0]
    num_of_points = int((end_time - start_time) / period)

    if num_of_points > 1:
        time_points = [start_time+period/2.0+i*period for i in range(num_of_points)]
        xs = np.interp(time_points, seq[:,0], seq[:,1])
        ys = np.interp(time_points, seq[:,0], seq[:,2])
        zs = np.interp(time_points, seq[:,0], seq[:,3])
        return np.stack((time_points, xs, ys, zs), axis=1)
    else:
        return seq[0:1,:]




def data_labeled_with_time(seq_len, verbose = True, shrink_percentage=1):
    '''
    ignore @shrink_percentage for now.

    @return: (accel_data, gyro_data). an element of the tupe is
        a dict, with key being the label, and item is numpy array of 
        shape [batch_num, 4, seq_len, 1]. for 4, 0->time, 1->x, 2->y, 3->z.
    '''
    label_file = open(os.path.join(os.path.dirname(__file__), 'labels.txt'), 'r')
    lines = label_file.read().split('\n')
    label_to_filenames = {} # label => [tuple]. tuple[0] is accelerometer, tuple[1] is gyro
    for line in lines:
        tokens = line.split('=>')
        try:
            file_num = int(tokens[0])
            label = tokens[1].strip()
            if label not in label_to_filenames:
                label_to_filenames[label] = []
        except Exception as err:
            if verbose:
                print('skip a line: '+line)
            continue
        accel_file = 'accelerometer_{0}.csv'.format(str(file_num))
        accel_file = os.path.join(os.path.dirname(__file__), accel_file)
        gyro_file = 'gyroscope_{0}.csv'.format(str(file_num))
        gyro_file = os.path.join(os.path.dirname(__file__), gyro_file)
        label_to_filenames[label].append((accel_file, gyro_file))

    # read data from files
    accel_data_labeled = {}
    gyro_data_labeled = {}
    for label, filenames in label_to_filenames.items():

        accel_data_labeled[label] = []
        for accel_fn, _ in filenames:
            # accelerometer
            accel_file = open(accel_fn, 'r')
            accel_file.readline()
            for line in accel_file.read().split('\n'):
                tokens = line.split(',')
                try:
                    x = float(tokens[0])
                    y = float(tokens[1])
                    z = float(tokens[2])
                    time = float(tokens[3]) / 1000000000
                except Exception as err:
                    if verbose:
                        print('accel skip a line: '+line)
                    continue
                accel_data_labeled[label].append(np.array([time, x, y, z]))

        gyro_data_labeled[label] = []
        for _, gyro_fn in filenames:
            # gyro scope
            gyro_file = open(gyro_fn, 'r')
            gyro_file.readline()
            for line in gyro_file.read().split('\n'):
                tokens = line.split(',')
                try:
                    x = float(tokens[0])
                    y = float(tokens[1])
                    z = float(tokens[2])
                    time = float(tokens[3]) / 1000000000
                except Exception as err:
                    if verbose:
                        print('gyro skip a line: '+line)
                    continue
            gyro_data_labeled[label].append(np.array([time,x,y,z]))

    # numpify *_data_labeled
    for label, data in accel_data_labeled.items():
        accel_data_labeled[label] = np.stack(data)
    for label, data in gyro_data_labeled.items():
        gyro_data_labeled[label] = np.stack(data)

    # now interpolate *_data_labeled
    for label, data in accel_data_labeled.items():
        accel_data_labeled[label] = _interpolate2(data, 0.02) # 50HZ
    for label, data in gyro_data_labeled.items():
        gyro_data_labeled[label] = _interpolate2(data, 0.02)  # 50HZ

    # convert *_data_labeled according to seq_len
    used_interpolate2 = True
    if used_interpolate2:
        for label, data in accel_data_labeled.items():
            batch_num = int(data.shape[0]/seq_len)
            batches = [data[i*seq_len:(i+1)*seq_len,:] for i in range(batch_num)]
            data = np.stack(batches, axis=0)
            data = np.expand_dims(data, axis=-1)
            data = np.swapaxes(data, 1, 2)
            accel_data_labeled[label] = data

        for label, data in gyro_data_labeled.items():
            batch_num = int(data.shape[0]/seq_len)
            batches = [data[i*seq_len:(i+1)*seq_len,:] for i in range(batch_num)]
            data = np.stack(batches, axis=0)
            data = np.expand_dims(data, axis=-1)
            data = np.swapaxes(data, 1, 2)
            gyro_data_labeled[label] = data
    else:
        raise Exception("not yet implemented...")



    return accel_data_labeled, gyro_data_labeled


def data_labeled(seq_len, verbose = True, shrink_percentage=1):
    '''
    ignore @shrink_percentage for now.

    @return: (accel_data, gyro_data). an element of the tupe is
        a dict, with key being the label, and item is numpy array of 
        shape [batch_num, 3, seq_len, 1]. for 3, 0->x, 1->y, 2->z.
    '''

    accel_data_labeled, gyro_data_labeled = data_labeled_with_time(seq_len, verbose=verbose, 
                                        shrink_percentage=shrink_percentage)
    for label, data in accel_data_labeled.items():
        accel_data_labeled[label] = data[:,1:4,:,:]
    for label, data in gyro_data_labeled.items():
        gyro_data_labeled[label] = data[:,1:4,:,:]

    return accel_data_labeled, gyro_data_labeled
