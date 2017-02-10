## motion_basis_learning
see `../train_model.py` as an example how to use it.

## descriptor_extractor
given parameters related to computing basis, it extracts descriptor of data.
example usage
```python
import numpy as np
from descriptor_extractor import DescriptorExtractor

config = {
        'k': 50,
        'filter_width': 5,
        'pooling_size': 4,

        'restore_path': '../variables_saved/accel/variables-69',
        'param_scope_name': 'variables_saved/accel/variables'
}
extractor = DescriptorExtractor(config)

dummy_data = np.ones((3, 100))
descriptor = extractor.extract_descriptor(dummy_data)
```

## model_eval
a script to evaluate if motions basis is effective or not
```shell
# to run it
python model_eval.py
```