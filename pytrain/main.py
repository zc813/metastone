import os
import numpy as np
import time
from models import NaiveModel, LinearModel
from callbacks import SaveOnTrainingEnd, TensorBoard, ExportGraphOnTrainingEnd
from keras.optimizers import RMSprop
from data import DataLoader
import argparse

numFeatures = 86
dataPath = os.path.expanduser("~/metastone/app/build/resources/main/selfplay/")
savePath = os.path.expanduser("~/metastone/app/build/resources/main/models/latest_model.h5")
exportPath = os.path.expanduser("~/metastone/app/build/resources/main/models/")

def getInputShape():
    return [numFeatures] # model input : one-dimensional features

def run(lr, verbose, batchSize, epochs, retireRate, loadThreshold):
    # prepare data
    dataLoader = DataLoader(removal=True)  # removal=True: remove already loaded data
    x = np.ndarray([0, numFeatures])
    y = np.ndarray([0,])

    # prepare model
    model = LinearModel(getInputShape())

    model.compile(RMSprop(lr=lr), 'mean_squared_error', ['accuracy'])
    model.summary()

    # load current best if it exists
    if os.path.isfile(savePath):
        model.load_weights(savePath)

    saveCallback = SaveOnTrainingEnd(savePath)
    tensorBoardCallback = TensorBoard(batch_size=batchSize)
    exportCallback = ExportGraphOnTrainingEnd(exportPath)

    while True:
        # load data
        data = dataLoader.iterDir(dataPath, min=loadThreshold) # update min num of entries each time, wait otherwise
        dataLoaded = np.array(data)

        # the last index is for y(label)
        xLoaded, yLoaded = dataLoaded[:,:-1], dataLoaded[:,-1]

        # retire current data. append new data.
        sampledIdx = np.random.random([len(x)]) > retireRate
        x, y = np.concatenate([x[sampledIdx], xLoaded]), np.concatenate([y[sampledIdx], yLoaded])

        print("Loaded (%d, %d) / (%d, %d)" % (len(xLoaded), len(yLoaded), len(x), len(y)))

        # train
        model.fit(x, y, batchSize, epochs, verbose=verbose,
            callbacks=[saveCallback,
                       exportCallback,
                       tensorBoardCallback])

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("--lr", type=float, default=0.001, help="Learning rate")
    parser.add_argument("-v", "--verbose", type=int, default=0,
                        help="0, 1, or 2. Level of verbose")
    parser.add_argument("--retire", type=float, default=0.1,
                        help="Retire rate r. After each training, retire a \
                        certain percentage of data. So that the size of total \
                        training data set will converge to avg(data_loaded)/\
                        (1-r). For example if r=0.1, and each time 600 entries \
                        of data are loaded, then the final training size will \
                        converge to 6000.")
    parser.add_argument("--batch", type=int, default=32, help="Batch size")
    parser.add_argument("--loadthreshold", type=int, default=500,
                        help="Min num of entries fetched (update interval)")
    parser.add_argument("--epoch", type=int, default=20,
                        help="Epochs for each training procedure")
    args = vars(parser.parse_args())

    run(lr=args.get("lr"),
        verbose=args.get("verbose"),
        batchSize=args.get("batch"),
        epochs=args.get("epoch"),
        retireRate=args.get("retire"),
        loadThreshold=args.get("loadthreshold"))
