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

		self.w = basis_learned.w
		self.hb = basis_learned.vb
		self.axis_num = basis_learned.axis_num
		self.sess = basis_learned.sess

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
		axis_num = self.axis_num
		hb = self.hb
		w = self.w
		sess = self.sess

		assert(axis_num == data.shape[0] and len(data.shape) == 2)

		data_fitted = tf.convert_to_tensor(data, dtype=tf.float32)
		data_fitted = tf.expand_dims(tf.expand_dims(data_fitted, axis=0), axis=-1)
		convolution = tf.nn.convolution(data_fitted, w, 'VALID', strides=[1,1])
		probabilities = tf.squeeze(tf.nn.sigmoid(convolution + hb))
		probabilities = sess.run(probabilities)

		descriptor_raw = np.zeros((probabilities.shape[1], probabilities.shape[0]))
		for k in range(probabilities.shape[1]):
			for i in range(probabilities.shape[0]):
				p = probabilities[i, k]
				descriptor_raw[k, i] = np.random.choice([0,1], p=[1-p, p])
		return descriptor_raw





		