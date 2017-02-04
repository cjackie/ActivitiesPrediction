# What is it
machine learning algorithms ralted code.
- `model_traning`, feature extraction. We used RBM.
- `index_construction`, make indexes of motions data, and perform clustering
- `variables_save`, parameters of our model. produced by running `train_model.py`
- `summaries`, training output to visualize the progress of training.

# Installation
have pip and virtualenv availabe on your OS, then
```shell
pip install -r requirements.txt
```

# Training
download dara first
```shell
cd data/shoaib_data_set
./download.sh
```
configure `train_model.py` if needed, then
```shell
python train_model.py
```