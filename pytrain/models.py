from keras.models import Sequential
from keras.layers import Dense
import sys
import numpy as np
from keras import backend as K
import tensorflow as tf
import os

class NaiveModel(Sequential):
    def __init__(self, input_shape):
        super().__init__()
        self.add(Dense(80, activation='relu', input_shape=input_shape))
        self.add(Dense(40, activation='relu'))
        self.add(Dense(20, activation='relu'))
        self.add(Dense(10, activation='relu'))
        self.add(Dense(4, activation='relu'))
        self.add(Dense(1, activation='sigmoid'))


class LinearModel(Sequential):
    def __init__(self, input_shape):
        super().__init__()
        self.add(Dense(1, activation='sigmoid', input_shape=input_shape))




###############################################################
# Below are codes for generating initialization model weights #
# as a starting point for java to self-play. Copy the genera- #
# ted files to your model folder.                             #
###############################################################

def saveModelToGraph(model, path):
    sess = K.get_session()
    builder = tf.saved_model.builder.SavedModelBuilder(path)
    x = tf.saved_model.utils.build_tensor_info(model.input)
    y = tf.saved_model.utils.build_tensor_info(model.output)
    signature = tf.saved_model.signature_def_utils.build_signature_def(
        inputs={'input': x},
        outputs={'output': y},
        method_name=tf.saved_model.signature_constants.PREDICT_METHOD_NAME,
    )
    builder.add_meta_graph_and_variables(
        sess, ['model'],
        signature_def_map={'signature': signature},
        # legacy_init_op=self.legacy_init_op
    )
    builder.save()

if __name__ == "__main__":
    # fp = sys.argv[1]
    model = LinearModel((86,))
    model.compile('sgd', 'mean_squared_error')
    if True:
        # model.load_weights(os.path.expanduser("~/metastone/app/build/resources/main/models/latest_model.h5"))
        pass
    else:
        # initialize to game tree feature values
        model.set_weights([np.reshape(np.array([-0.042435191603752365, 0.40616696103829747, -0.6076077508888231,
                                             0.7272720043082649, 0.20202105394258138, 0.6313194790542382,
                                             0.018128759542628426, -0.6714877445764238, -1.4053075565764823,
                                             -0.5645665894497521, 0.7847370830575832, 0.6027815061309918,
                                             0.11944932690773696, 0.16639617889671887, -0.596884142191235,
                                             -1.303442871303662, -0.7689279203049311, 0.08684843915912507,
                                             0.2194776555521321, 0.3016746685481717, 1.2937229089380362,
                                             -0.3867675424274616, 0.1076755527545435, -0.2732232995785765,
                                             -1.2185303120526827, 0.44953477632896055, 0.4381595843767112,
                                             -0.42740268057811853, -0.7593509953652382, -0.25454178333555133,
                                             -0.06777467174623754, -0.29690084326922533, 0.139006279981216,
                                             -0.19757466435610388, -0.10281368164119024, -1.0318842806816224,
                                             0.6697091092076944, -1.5504887549148079, -0.46513049548185065,
                                             -0.224168675085325, 0.3363897344845124, 1.0077572813673379,
                                             1.790712443198513, -0.7974532011373017, -0.034379835513210874,
                                             -0.53728431299419, 0.27911211300832656, 1.77296275442259,
                                             -0.49676110743592145, 0.1543295718827667, -0.8465960481972349,
                                             0.7616942260912617, -0.09467554276581093, -2.0242808280175053,
                                             -0.42044884895501194, 0.22098060325157623, -0.7124548934850738,
                                             -0.46505479523846677, 0.48943013064063173, -1.0814450906779396,
                                             1.494202436696506, -0.2253465154462635, -0.45375551865466784,
                                             0.23376970921025383, -0.31330715778322343, -0.36119276257174077,
                                             -0.643335795514266, -9.750616593995213E-4, 0.38910933337215503,
                                             0.09884268820871578, 0.37335447500509883, 0.8047764493080031,
                                             -0.4720077472112548, 0.264490464098162, 0.4104466046033848,
                                             -1.3686731855897478, -0.3840621668768126, 1.0668114179340098,
                                             0.2664341808467372, -0.9317865799520276, 0.7754792723230315,
                                             0.23662829519630627, -0.4460817116862948, -0.010734604936549205,
                                             0.08007887984653188, -0.7490589136711456, ], dtype=np.float32), (86,1)),
                       np.array([0], dtype=np.float32)])

    model.save_weights('latest_model.h5')
    saveModelToGraph(model, "0")

    print(model.get_weights())
