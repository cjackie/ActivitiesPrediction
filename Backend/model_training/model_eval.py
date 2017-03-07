from descriptor_extractor import DescriptorExtractor
from sklearn import svm
import numpy as np
import random 

from ..data.shoaib_data_set import process as shoaib_process
from ..data.my_data import process as my_process

verbose = True
config = {
    'k': 50,
    'filter_width': 5,
    'pooling_size': 4,

    'restore_path': '../variables_saved/accel/variables-69', # path to parameters
    'param_scope_name': 'variables_saved/accel/variables'
}
accel_basis_extractor = DescriptorExtractor(config)

# preparing test data nd training data
test_percentage = 0.1 # percentage of data being test data.
accel_data, _ = shoaib_process.data_labeled(100,verbose=verbose,shrink_percentage=0.02)
max_samples_num = 0
for _, data in accel_data.items():
    max_samples_num += data.shape[0]
train_data_num = 0
train_data, train_labels = np.ones((max_samples_num, config['k'])), ['']*max_samples_num
test_data_num = 0
test_data, test_labels = np.ones((max_samples_num, config['k'])), ['']*max_samples_num
if verbose:
    print("loading data and extracting descriptors:")
    progress_i = 0
    progress_max = 100 if max_samples_num > 100 else max_samples_num
    progress = [' ']*progress_max
    out = '[{0}]'.format(''.join(progress))
    sys.stdout.write(out+'\r')
    sys.stdout.flush()

for label, data in accel_data.items():
    for i in range(data.shape[0]):
        if random.random() < test_percentage:
            # test sample
            test_data[test_data_num,:] = accel_basis_extractor.extract_descriptor(data[i,:,:,0])
            test_labels[test_data_num] = label
            test_data_num += 1
        else:
            # train sample
            train_data[train_data_num,:] = accel_basis_extractor.extract_descriptor(data[i,:,:,0])
            train_labels[train_data_num] = label
            train_data_num += 1

        if verbose:
            progress_made = progress_i % (max_samples_num/progress_max) == 0
            over_progress = progress_i / (max_samples_num/progress_max) >= progress_max
            if progress_made and not over_progress:
                progress[progress_i / (max_samples_num/progress_max)] = '#'
                out = '[{0}]'.format(''.join(progress))
                sys.stdout.write(out+'\r')
                sys.stdout.flush()
            elif progress_made and not over_progress:
                print("warning... unexpected.")
            progress_i += 1

train_data, train_labels = train_data[0:train_data_num,:], train_labels[0:train_data_num]
test_data, test_labels = test_data[0:test_data_num,:], test_labels[0:test_data_num]

# loading my data
if verbose:
    print 'loading my data, and creating descriptiors....'
my_accel_data, _ = my_process.data_labeled(100, verbose=True)
my_data, my_labels = [], []
for label, data in my_accel_data.items():
    my_data_num = data.shape[0]
    for i in range(my_data_num):
        descriptor = accel_basis_extractor.extract_descriptor(data[i,:,:,0])
        my_data.append(descriptor)
    my_labels.extend([label]*my_data_num)

my_data = np.stack(my_data, axis=0) 
if verbose:
    print('finished.')


# support vector machine
if verbose:
    print('training')
classifier = svm.SVC()
# fitting data
classifier.fit(train_data, train_labels)

# validate with test data
result = classifier.predict(test_data)
correct_num = 0
for i in range(test_data_num):
    if result[i] == test_labels[i]:
        correct_num += 1
if verbose:
    print("predicted labels:")
    print(result)
    print("actual labels:")
    print(test_labels)
print('correct rate is: {0}.'.format(str(float(correct_num)/test_data_num)))

#validate with my data
print('validation with my data')
my_result = classifier.predict(my_data)
my_correct_num = 0
for i in range(len(my_labels)):
    if my_result[i] == my_labels[i]:
        my_correct_num += 1
if verbose:
    print('predicted labels:')
    print(my_result)
    print('actual labels:')
    print(my_labels)
print('correct rate is: {0}.'.format(str(float(my_correct_num)/len(my_labels))))



