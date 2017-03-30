from model_training.descriptor_extractor import DescriptorExtractor
from sklearn import svm
import numpy as np
import os

from data.local_data.process import PERIOD, interpolate2, data_labeled


class _Model():
    def __init__(self, svm, basis_config, accel_basis, seq_len):
        self.__svm = svm
        self.__basis_config = basis_config
        self.__accel_basis = accel_basis
        self.__seq_len = seq_len

    def _predict(self, data):
        '''
        produce lables given data
        :param data: numpy array. descriptors. shape of (n, feature_size).
        :return: array of string. they are labels.
        '''
        return self.__svm.predict(data)

    def _predict_raw(self, data_raw):
        '''
        given just primitive data
        :param data_raw: numpy array. shape (batch_size, 3, seq_len, 1)
        :return: array of string. the are labels for each batch.
        '''
        descriptors_u = []
        for i in range(data_raw.shape[0]):
            descriptors_u.append(self.__accel_basis.extract_descriptor(data_raw[i,:,:,0]))
        descriptors = np.stack(descriptors_u)
        return self._predict(descriptors)

    def predict_with_time(self, data):
        '''

        :param data: numpy array. shape is (n,4). n sequence length. 4: time, x, y, z.
        :return: String. label
        :exception: invalid input.
        '''
        data_interped = interpolate2(data, PERIOD)
        if data_interped.shape[0] < self.__seq_len:
            raise Exception("invalid_input")

        # prepare for extraction
        data = data_interped[:,1:4]
        data = np.swapaxes(data, 0, 1)

        descriptor = self.__accel_basis.extract_descriptor(data)
        descriptor = np.expand_dims(descriptor, 0)
        return self.__svm.predict(descriptor)[0]

    @property
    def basis_config(self):
        return self.__basis_config.copy()

    @staticmethod
    def empty_model():
        class EmptyModel:
            def predict_with_time(self, data):
                return 'NA'

            @property
            def basis_config(self):
                return {}

        EmptyModel()


# model
def get_default_model(verbose=False):
    # configurable variable
    basis_config = {
        'k': 50,
        'filter_width': 5,
        'pooling_size': 4,
        'restore_path': os.path.join(os.path.dirname(__file__),
                                     'variables_saved/accel/variables-357'), # path to parameters
        'param_scope_name': 'variables_saved/accel/variables'
    }
    shrink_percentage = 1
    seq_len = 120

    # start initialization
    # preparing data
    if verbose:
        print('loading data...')
    accel_basis = DescriptorExtractor(basis_config)
    accel_data, _ = data_labeled(seq_len, verbose=verbose, shrink_percentage=shrink_percentage)
    data = []
    labels = []
    for label, a_data in accel_data.items():
        for i in range(a_data.shape[0]):
            data.append(accel_basis.extract_descriptor(a_data[i,:,:,0]))
            labels.append(label)

    if len(data) == 0:
        return _Model.empty_model()
    else:
        # train model with the data
        if verbose:
            print('training...')
        svm_model = svm.SVC()                  # we use simple support vector machine
        svm_model.fit(data, labels)

        return _Model(svm_model, basis_config, accel_basis, seq_len)


