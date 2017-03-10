import numpy as np
import os
import re

PERIOD = 0.02  # in seconds

def interpolate2(seq, period):
    '''

    :seq: np array of shape (n, 4). where seq[:,0], is time(unit second) in
        increasing order, seq[:,1] is x, seq[:,2] is y, seq[:,3] is z.
    :period: double. time between adjacent data point. unit second.
    :return: numpy array. it has shape [?, 4]
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


def data_labeled(seq_len, verbose = True, shrink_percentage=1):
    '''
    :seq_len: int.
    :verbose: bool.
    :shrink_percentage: ignored.
    :return: (accel_data, gyro_data). an element of the tupe is
        a dict, with key being the label, and item is numpy array of
        shape [batch_num, 3, seq_len, 1]. for 3, 0->x, 1->y, 2->z.
    :exception.
    '''

    # getting all label to filenames mapping.
    label_file_path = os.path.join(os.path.dirname(__file__), 'labels.txt')
    if not os.path.isfile(label_file_path):
        raise Exception('data not available')
    label_to_filenames = {} # label => filenames. accelerometer,
    with open(label_file_path, 'r') as f:
        _raw = f.read()
        _reg = r'([a-zA-Z0-9_]+\.csv),([a-zA-Z0-9]+)'
        for _match in re.finditer(_reg, _raw):
            _filename = _match.group(1)
            _label = _match.group(2)
            if not label_to_filenames.has_key(_label):
                label_to_filenames[_label] = []
            label_to_filenames[_label].append(_filename)

    # read data from files
    accel_data_labeled = {}
    for _label, _filenames in label_to_filenames.items():
        if not accel_data_labeled.has_key(_label):
            accel_data_labeled[_label] = []

        for _fn in _filenames:
            _file = open(os.path.join(os.path.dirname(__file__), _fn), 'r')
            _reg = r'([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+),' \
                   r'([-+]?\d*\.\d+|[-+]?\d+),([-+]?\d*\.\d+|[-+]?\d+)'
            _data = []
            for _match in re.finditer(_reg, _file.read()):
                _time = float(_match.group(1))/1000000000
                _x = float(_match.group(2))
                _y = float(_match.group(3))
                _z = float(_match.group(4))
                _data.append([_time, _x, _y, _z])

            _data = np.array(_data)
            if _data.shape[0] > 0:
                # interpolate
                _data = interpolate2(_data, PERIOD)

            if _data.shape[0] > seq_len:
                _batch_num = int(_data.shape[0]/seq_len)
                _batches = [_data[i*seq_len:(i+1)*seq_len,:] for i in range(_batch_num)]
                _data = np.stack(_batches, axis=0)
                _data = np.expand_dims(_data, axis=-1)
                _data = np.swapaxes(_data, 1, 2)
                _data = _data[:,1:4,:,:] # remove time
                accel_data_labeled[_label].append(_data)

    # concat together
    for _label, _data in accel_data_labeled.items():
        if _data == []:
            raise Exception('not enough data of {0}'.format(_label))
        accel_data_labeled[_label] = np.concatenate(_data)

    return accel_data_labeled, None


# poor man testing
if __name__ == '__main__':
    data_labeled(10)
