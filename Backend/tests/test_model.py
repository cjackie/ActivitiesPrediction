import sys
import numpy as np
sys.path.append('..')
from model import get_default_model

def test():
    model = get_default_model(True)
    data = [np.random.random_sample(model.basis_config['k'])]
    print(str(model.predict(data)))
    return True

if __name__=='__main__':
    test()
