# -*- coding: utf-8 -*-
"""
Created on Wed Jun 28 17:08:13 2017

@author: sjxn2423

考虑多种reward和Q-function对应机制：
1. 完整一局对局后，直接根据最后的reward计算total discounted rewards，作为Q(s_t,a_t,w)的对应标签，训练模型中的参数w  （类似于supervised方法，只是现在增量训练，慢慢更新）
2. 想办法给一局中的中间步骤定义reward（比如根据一步之后双方的Hp变化量），然后使用Bellman最优方程，优化 L(w) = [r_t + gamma*max_a{Q(s_t+1, a, w)} - Q(s_t, a_t, w)]^2
3. 使用类似Policy Gradient的思想，完整的运行一局之后计算total discounted rewards，并减去均值，除以标准差(记为v_t)，然后往 v_t*Grad_w(Q(s_t,a_t,w))方向更新，
   也就是说如果s_t下执行a_t对应的v_t>0, 就更新参数w，使得Q(s_t,a_t,w)增大，反之更新参数使之减小， Q-function最后可以加上sigmoid函数防止函数值unbounded, 
   游戏内选择action也可以用softmax的方式概率选择，而不是直接选Q_value最大的 （方案1、2也可以尝试get_softmax_action的方式选择action，而不是用epsi-greedy）
   

可以参考 Sutton书本的Chapter9及以后部分，比如各种Semi-gradient Methods等

不同的reward机制：
1. 根据每一次action之后双方血量的相对变化， 在对random player的训练时很容易胜率到80%左右
2. 只根据最后胜负结果，指定reward为1或-1， 在对random player的训练时胜率似乎也能慢慢提升，相对较慢，

2017-07-12 发现 ReplayQLearning中的evaluateContext存在的没有使用环境中最新player状态的重大bug， 之前结果没有意义，需要重新实验
1. 暂时依然没有发现学习迹象

可以尝试用Sarsa的方式构造数据 Sutton P230
"""

import os
import cPickle as pickle
import random
#import numpy as np
from collections import defaultdict, deque
from sklearn.neural_network import MLPRegressor
#from sklearn.externals import joblib
from sklearn2pmml import PMMLPipeline
from sklearn2pmml import sklearn2pmml

data_file = 'E:\\MetaStone\\app\\report.log'
buffer_file = 'E:\\MetaStone\\app\\buffer.pkl'
model_file = 'E:\\MetaStone\\app\\mlp.model'
pmml_file = 'E:\\MetaStone\\app\\mlp.pmml'
buffer_size = 500
batch_size = 100

if os.path.exists(buffer_file):
    replay_buffer = pickle.load(open(buffer_file, 'rb'))
else:
    replay_buffer = deque(maxlen=buffer_size)

if os.path.exists(model_file):
#    mlp = joblib.load(model_file)
    mlp = pickle.load(open(model_file, 'rb'))
    mlp.set_params(learning_rate_init=0.0002)  #尝试减小更新步子
else:
    mlp = MLPRegressor(hidden_layer_sizes=(5,), learning_rate_init=2e-4, tol=1e-6, random_state=3)

def get_discounted_data(data_dict, final_hp_diff, total_turn, final_env_state):
    """提取完整一局的特征数据和对应标签
    考虑discounted reward"""
    gamma = 0.99
    final_hp_diff = final_hp_diff/abs(final_hp_diff) #reward只关注胜负，不关注Hp差值，和supervised训练label吻合
    for turn in range(0, total_turn):
        replay_buffer.append((data_dict[turn]['envState'], gamma**(total_turn-turn)*final_hp_diff))
    replay_buffer.append((final_env_state, final_hp_diff))
      
def get_immediate_reward(state_next, state):
    """根据连续状态下Hp的相对变化计算reward"""
    r = state_next[0]-state_next[15] - (state[0]-state[15])
    return r/30.0

def get_Q_learn_data(data_dict, final_hp_diff, total_turn, final_env_state):
    """根据Bellman方程，提取完整一局的特征数据和对应标签, 
    immediate reward使用执行action之后带来的双方Hp变化量"""
    gamma = 0.99
    for turn in range(1, total_turn):
        reward = get_immediate_reward(data_dict[turn]['envState'], data_dict[turn-1]['envState'])
        target = reward + gamma*data_dict[turn]['bestScore']
        replay_buffer.append((data_dict[turn]['envState'], target))        
    replay_buffer.append((final_env_state, final_hp_diff/30.0))
#    replay_buffer.append((final_env_state, final_hp_diff/abs(final_hp_diff)))  # 根据胜负指定final reward
        
def load_data(file_name, data_type):
    data_dict = defaultdict(dict)
    with open(file_name, 'r') as f:
        for line in f:
            if line[0] != '{':
                continue            
            info = eval(line)            
            if 'winner' in info: #一局结束, 提出完整一局的特征数据和对应标签
                if data_type == 1:
                    get_discounted_data(data_dict, info['HpDiff'], info['Turn'], info['envState'])
                elif data_type == 2:
                    get_Q_learn_data(data_dict, info['HpDiff'], info['Turn'], info['envState'])
                else:
                    pass
            else:
                data_dict[info['Turn']]['envState'] = info['envState']
                data_dict[info['Turn']]['bestScore'] = info['bestScore']
    # 保存replay_buffer到文件
    pickle.dump(replay_buffer, open(buffer_file, 'wb'))                

if __name__ == '__main__':
    
    data_type = 1
    load_data(data_file, data_type)
    # 清空report.log
    with open(data_file, 'w') as f:
        f.write('')
    
    # 从replay_buffer随机采样数据, 增量更新模型 
    # (可以考虑修改成维护一个200的buffer，然后每次用最新的200个数据点更新模型，相对于每次只用最新一局的数据而言，应该能smooth一些，不会波动太大)
    sample_size = min(batch_size, len(replay_buffer))
    mini_batch = random.sample(replay_buffer, sample_size)
    X_batch = [data[0] for data in mini_batch]
    y_batch = [data[1] for data in mini_batch]
    
    # partial_fit是incremental更新，看了partial_fit的源代码，它更新一步之后就break了，所以和max_iter无关，它主要受learning_rate_init的数值影响，就是步子的大小
    mlp.partial_fit(X_batch, y_batch)
#    print mlp.intercepts_
#    mlp.predict(X_batch[:5])
    
    print "number of samples: {}".format(len(y_batch))
#    joblib.dump(mlp, model_file)  #发现使用joblib保存模型再加载出来以后，调用partial_fit训练模型不会再更新，原因不明，大坑！！！
    pickle.dump(mlp, open(model_file, 'wb'))     

    # dump the model to PMML file
    mlp_pipeline = PMMLPipeline([("Q_model", mlp)])
    sklearn2pmml(mlp_pipeline, pmml_file, with_repr = True)