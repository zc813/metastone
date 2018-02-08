import json
import csv
import sys

print(sys.argv)

filename = sys.argv[1]
writeto = 'a.csv'

jsonDecoder = json.JSONDecoder()

with open(filename) as f:
    res = jsonDecoder.decode(f.read())
    print(res)

with open(writeto, 'w') as f:
    writer = csv.writer(f)
    for entry in res:
        writer.writerow(entry)