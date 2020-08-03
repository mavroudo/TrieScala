#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Aug  3 16:38:02 2020

@author: mavroudo
"""
import random
import string
import sys
from random import randrange
import datetime 


delimiter="/delab/"

def event_generator(size=8, chars=string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))

def createSequence(l: list, length: int):
    return random.choices(l, k=length)

def createTxt(m,maxLength,l,fileName):
    chars = [event_generator() for i in range(l)]
    sequences=[]
    for _ in range(m):
        sequences.append(createSequence(chars, random.randint(1,maxLength)))
    
    with open(fileName,"w") as f:
        for sequence in sequences:
            f.write(",".join(sequence)+"\n")

def random_date(start,l):
   current = start
   while l >= 0:
      curr = current + datetime.timedelta(minutes=randrange(60))
      yield curr.strftime("%Y-%m-%d %H:%M:%S")
      l-=1
            
def createWithTimestamp(m,maxLength,l,startDate,fileName):
    chars = [event_generator() for i in range(l)]
    sequences=[]
    timestamps=[]
    for _ in range(m):
        length=random.randint(1,maxLength)
        sequences.append(createSequence(chars, length))
        timestamps.append([i for i in random_date(startDate,length)])
        
    with open(fileName,"w") as f:
        for timestamp,sequence in zip(timestamps,sequences):
            a_string=""
            for t,e in zip(timestamp,sequence):
                a_string+=e+delimiter+t+","
            f.write(a_string[:-1]+"\n")
    
if __name__ == "__main__":
    arguments=sys.argv
    m=int(arguments[1])
    maxLength=int(arguments[2])
    l=int(arguments[3])
    fileName=arguments[4]
    
    if fileName.split(".")[1]=="txt":
        createTxt(m,maxLength,l,fileName)
    else:
        startDate = datetime.datetime(2020, 8, 15 ,12,00) # hard coded but can change
        createWithTimestamp(m,maxLength,l,startDate,fileName)
    
   