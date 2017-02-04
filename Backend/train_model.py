####################################################
# use this script to get parameters for extracting 
# descriptor
##################################

from model_training.motion_basis_learning import MotionBasisLearner
import numpy as np

from data.shoaib_data_set import process

config = {
	'time_length': 600, 
	'k': 50, 
	'filter_width': 5, 
	'pooling_size': 4,

	'accelerometer_restore_path': None,
	'gyroscope_restore_path': None,
	'accelerometer_variable_path': 'variables_saved/accel/variables',
	'accelerometer_summaries_dir': 'summaries/accel/',
	'gyroscope_variable_path': 'variables_saved/gyro/variables',
	'gyroscope_summaries_dir': 'summaries/gyro/'
}


# getting data
accelerometer_data, gyroscope_data = process.data(config['time_length'])

# init model
accel_basis = MotionBasisLearner(k=config['k'], filter_width=config['filter_width'],
		pooling_size=config['pooling_size'], save_params_path=config['accelerometer_variable_path'],
		summary_dir=config['accelerometer_summaries_dir'], param_scope_name=config['accelerometer_variable_path'])
gyro_basis = MotionBasisLearner(k=config['k'], filter_width=config['filter_width'],
		pooling_size=config['pooling_size'], save_params_path=config['gyroscope_variable_path'],
		summary_dir=config['gyroscope_summaries_dir'], param_scope_name=config['gyroscope_variable_path'])

# init training
summary_flush_secs = 120
save_interval = 20
accel_basis.build_training_model(accelerometer_data, restore_params_path=config['accelerometer_restore_path'], 
		summary_flush_secs=summary_flush_secs, save_interval=save_interval)
gyro_basis.build_training_model(gyroscope_data, restore_params_path=config['gyroscope_restore_path'],
		summary_flush_secs=summary_flush_secs, save_interval=save_interval)

# start training
learning_rate = 0.01
steps = 10
verbose = True
while True:
	if verbose:
		print("learning acceleromenter basis:")
	accel_basis.train(verbose=verbose, steps=steps, learning_rate=learning_rate)

	if verbose:
		print("learning gyroscope basis:")
	gyro_basis.train(verbose=verbose, steps=steps, learning_rate=learning_rate)

	