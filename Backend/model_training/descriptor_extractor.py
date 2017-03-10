import tensorflow as tf
import numpy as np

from motion_basis_learning import MotionBasisLearner

class DescriptorExtractor():

    def __init__(self, config):
        '''
        @config: dict. has following keys:
                `k`: int. model related.
                `filter_width`: int. model related.
                `pooling_size`: int. model related.
                `restore_path`: string. path to parameters.
                `param_scope_name`: string. scope name for variables...
        '''
        basis_learned = MotionBasisLearner(k=config['k'], filter_width=config['filter_width'], 
                            pooling_size=config['pooling_size'], param_scope_name=config['param_scope_name'])
        basis_learned.restore_model(config['restore_path'])

        data_fitted = tf.placeholder(tf.float32, shape=[1,3,None,1])
        convolution = tf.nn.convolution(data_fitted, basis_learned.w, 'VALID', strides=[1,1])
        probabilities = tf.squeeze(tf.nn.sigmoid(convolution + basis_learned.hb))

        self.probabilities = probabilities
        self.data_fitted = data_fitted
        self.sess = basis_learned.sess
        self.axis_num = basis_learned.axis_num

    def extract_descriptor(self, data):
        '''
        @data, numpy array. shape (@self.axis_num, len), where len is greater than
            w.shape[1]
        @return: numpy array. shape is (n,) where n is number of 
            features.
        '''
        descriptor_raw = self._extract_descriptor_raw(data)
        f = descriptor_raw.shape[0]
        t = descriptor_raw.shape[1]

        descriptor = np.zeros(f)
        for k in range(f):
            descriptor[k] = np.sum(descriptor_raw[k, :])/float(t)
        return descriptor

    def _extract_descriptor_raw(self, data):
        '''
        @data, numpy array. shape (@self.axis_num, len), where len is greater than
            w.shape[1]
        @return: numpy array. shape (k, m). k is number of features
        '''
        probabilities = self.probabilities
        data_fitted = self.data_fitted
        sess = self.sess
        axis_num = self.axis_num

        assert(axis_num == data.shape[0] and len(data.shape) == 2)

        data_fitted_feed = np.expand_dims(np.expand_dims(data,0),-1)
        probabilities_result = sess.run(probabilities, feed_dict={data_fitted: data_fitted_feed})

        descriptor_raw = np.zeros((probabilities_result.shape[1], probabilities_result.shape[0]))
        for k in range(probabilities_result.shape[1]):
            for i in range(probabilities_result.shape[0]):
                p = probabilities_result[i, k]
                descriptor_raw[k, i] = np.random.choice([0,1], p=[1-p, p])
        return descriptor_raw





