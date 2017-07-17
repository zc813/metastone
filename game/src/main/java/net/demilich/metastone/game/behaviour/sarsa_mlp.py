# -*- coding: utf-8 -*-
"""
Created on Thu Jul 13 11:27:38 2017

@author: sjxn2423

可以尝试用Sarsa的方式构造数据 Sutton P230

// 尝试使用Sarsa方法训练一个基于MLP的value function （sarsa本来是在线更新的，现在我们尝试一局batch更新一次，不确定理论上是否靠谱，后面可以尝试标准的在线更新Linear模型）
// 训练的loss函数： L(w) = [r_t + gamma*Q(s_t+1, a_t+1, w_fix) - Q(s_t, a_t, w)]^2
// 将执行action之后到达的局面s_t+1的描述向量作为 (s_t, a_t) 的描述, 训练目标变成 [r_t + gamma*Q(s_t+2, w_fix)} - Q(s_t+1, w)]^2
// 暂时考虑将我方的每一次action算作一个time step，我方EndTurn动作和对方完整执行以及下一个turn己方发牌合在一起算作一个time step （将对方当做环境），需要记录每一次操作的数据

// 后面也可以尝试将我方一个turn内的完整action path当做一个action，以一个完整回合作为一个time step，似乎更合理，因为我们更希望评估的是一系列操作之后的状态优劣
// sarsa似乎可以很容易改成那种action path定义，因为不需要像Q-Learning那样需要计算argmax，而直接follow epsi-greedy 策略即可

"""

import os
import cPickle as pickle
import random
from collections import defaultdict
from sklearn.neural_network import MLPRegressor
#from sklearn.externals import joblib
from sklearn2pmml import PMMLPipeline
from sklearn2pmml import sklearn2pmml

data_file = 'E:\\MetaStone\\app\\report.log'
model_file = 'E:\\MetaStone\\app\\mlp.model'
pmml_file = 'E:\\MetaStone\\app\\mlp.pmml'


data_buffer = []

if os.path.exists(model_file):
#    mlp = joblib.load(model_file)
    mlp = pickle.load(open(model_file, 'rb'))
    mlp.set_params(learning_rate_init=0.0002)  #尝试减小更新步子
else:
    mlp = MLPRegressor(hidden_layer_sizes=(5,), learning_rate_init=2e-4, tol=1e-6, random_state=3)
      
def get_immediate_reward(state_next, state):
    """根据连续状态下Hp的相对变化计算reward"""
    r = state_next[0]-state_next[15] - (state[0]-state[15])
    return r

def get_sarsa_data(data_dict, final_hp_diff, total_turn, final_env_state):
    """根据sarsa算法，提取完整一局的特征数据和对应标签, 
    immediate reward使用执行action之后带来的双方Hp变化量"""
    gamma = 0.99
    for turn in range(1, total_turn-1):
        reward = get_immediate_reward(data_dict[turn]['envState'], data_dict[turn-1]['envState'])
        target = reward + gamma*data_dict[turn+1]['QScore']
        data_buffer.append((data_dict[turn]['envState'], target))        
    data_buffer.append((data_dict[total_turn-1]['envState'], final_hp_diff))        
                         
def load_data(file_name):
    data_dict = defaultdict(dict)
    with open(file_name, 'r') as f:
        for line in f:
            if line[0] != '{':
                continue            
            info = eval(line)            
            if 'winner' in info: #一局结束, 提出完整一局的特征数据和对应标签
                get_sarsa_data(data_dict, info['HpDiff'], info['Turn'], info['envState'])
            else:
                data_dict[info['Turn']]['envState'] = info['envState']
                data_dict[info['Turn']]['QScore'] = info['QScore']         

if __name__ == '__main__':
    
    load_data(data_file)
    # 清空report.log
    with open(data_file, 'w') as f:
        f.write('')
    
    X_batch = [data[0] for data in data_buffer]
    y_batch = [data[1] for data in data_buffer]
    
    # partial_fit是incremental更新，看了partial_fit的源代码，它更新一步之后就break了，所以和max_iter无关，它主要受learning_rate_init的数值影响，就是步子的大小
    mlp.partial_fit(X_batch, y_batch)
#    print mlp.intercepts_
#    mlp.predict(X_batch[:5])
    
    print "number of samples: {}".format(len(y_batch))
    pickle.dump(mlp, open(model_file, 'wb'))     

    # dump the model to PMML file
    mlp_pipeline = PMMLPipeline([("Q_model", mlp)])
    sklearn2pmml(mlp_pipeline, pmml_file, with_repr = True)