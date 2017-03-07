from model_training.descriptor_extractor import DescriptorExtractor
from sklearn import svm
from importlib import import_module
import numpy as np
import os


class _Model():
    def __init__(self, svm, basis_config, accel_basis):
        self.__svm = svm
        self.__basis_config = basis_config
        self.__accel_basis = accel_basis

    def predict(self, data):
        '''
        produce lables given data
        :param data: numpy array. descriptors. shape of (n, feature_size).
        :return: array of string. they are labels.
        '''
        return self.__svm.predict(data)

    def predict_raw(self, data_raw):
        '''
        given just primitive data
        :param data_raw: numpy array. shape (batch_size, 3, seq_len, 1)
        :return: array of string. the are labels for each batch.
        '''
        descriptors_u = []
        for i in range(data_raw.shape[0]):
            descriptors_u.append(self.__accel_basis.extract_descriptor(data_raw[i,:,:,0]))
        descriptors = np.stack(descriptors_u)
        return self.predict(descriptors)

    @property
    def basis_config(self):
        return self.__basis_config.copy()


# model
def get_default_model(verbose=False):
    # configurable variable
    basis_config = {
        'k': 50,
        'filter_width': 5,
        'pooling_size': 4,
        'restore_path': os.path.join(os.path.dirname(__file__),
                                     'variables_saved/accel/variables-69'), # path to parameters
        'param_scope_name': 'variables_saved/accel/variables'
    }
    data_process_script = 'data.shoaib_data_set.process'
    shrink_percentage = 0.04

    # start initialization
    # preparing data
    if verbose:
        print('loading data...')
    accel_basis = DescriptorExtractor(basis_config)
    process = import_module(data_process_script)
    accel_data, _ = process.data_labeled(120, verbose=verbose, shrink_percentage=shrink_percentage)
    data = []
    labels = []
    for label, a_data in accel_data.items():
        for i in range(a_data.shape[0]):
            data.append(accel_basis.extract_descriptor(a_data[i,:,:,0]))
            labels.append(label)

    # train model with the data
    if verbose:
        print('training...')
    svm_model = svm.SVC()                  # we use simple support vector machine
    svm_model.fit(data, labels)

    return _Model(svm_model, basis_config, accel_basis)



