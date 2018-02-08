import os
import json
import time

class DataLoader:
    def __init__(self, removal=True):
        self.decoder = json.JSONDecoder()
        self.removal = removal

    def iterDir(self, dirpath, min=-1):
        result = []
        while True:
            with os.scandir(dirpath) as it:
                for entry in it:
                    if not entry.name.startswith('.') and entry.is_file():
                        result.extend(self.readFromFile(entry.path))
            if len(result)>min:
                return result

    def readFromFile(self, filename):
        if filename.startswith('.'):
            return
        result = []

        time.sleep(0.3) # wait for java to complete writing data

        with open(filename, mode='r') as f:
            s = f.readline()
            while s!="":
                result.extend(self.decoder.decode(s))
                s = f.readline()

        if self.removal:
            os.remove(filename)

        return result