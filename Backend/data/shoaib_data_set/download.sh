#! /usr/bin/env bash
wget http://ps.ewi.utwente.nl/Blog/Sensors_Activity_Recognition_DataSet_Shoaib.rar
unrar x Sensors_Activity_Recognition_DataSet_Shoaib.rar
cp DataSet/Participant_* ./
# rm 'Sensors_Activity_Recognition_DataSet_Shoaib.rar'
# rm -r 'DataSet'