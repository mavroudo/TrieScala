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
        sequence=createSequence(chars, length)
        timestamp=[i for i in random_date(startDate,length)]
        o_data=[(x,y) for y, x in sorted(zip(timestamp,sequence), key=lambda pair: pair[0])]
        sequences.append([i[0] for i in o_data])
        timestamps.append([i[1] for i in o_data])
    with open(fileName,"w") as f:
        index=0
        for timestamp,sequence in zip(timestamps,sequences):
            a_string=str(index)+"::"
            for t,e in zip(timestamp,sequence):
                a_string+=e+delimiter+t+","
            f.write(a_string[:-1]+"\n")
            index+=1

def read_with_timestamps(logfile):
    data=[]
    t=[]
    with open(logfile,"r") as f:
        for l in f:
            m=l.split("::")[1]
            for ev in m.replace("\n","").split(","):
                data+=[ev.split("/delab/")[0]]
                t+=[datetime.datetime.strptime(ev.split("/delab/")[1], "%Y-%m-%d %H:%M:%S")]
    return list(set(data)),max(t)
            
def create_additional_log(m,maxLength,filename):
    new_file_name=filename.split(".")[0]+"$2."+filename.split(".")[1]
    if filename.split(".")[1]=="withTimestamp":
        unique_data,ending_day=read_with_timestamps(filename)
        sequences=[]
        timestamps=[]
        for _ in range(m):
            length=random.randint(1,maxLength)
            sequence=createSequence(unique_data, length)
            timestamp=[i for i in random_date(ending_day,length)]
            o_data=[(x,y) for y, x in sorted(zip(timestamp,sequence), key=lambda pair: pair[0])]
            sequences.append([i[0] for i in o_data])
            timestamps.append([i[1] for i in o_data])
        with open(new_file_name,"w") as f:
            index=0
            for timestamp,sequence in zip(timestamps,sequences):
                a_string=str(index)+"::"
                for t,e in zip(timestamp,sequence):
                    a_string+=e+delimiter+t+","
                f.write(a_string[:-1]+"\n")
                index+=1 
    
    
    
    
if __name__ == "__main__":
    arguments=sys.argv
    create_extend=arguments[1]
    if create_extend =="create":
        m=int(arguments[2])
        maxLength=int(arguments[3])
        l=int(arguments[4])
        fileName=arguments[5]
        if fileName.split(".")[1]=="txt":
            createTxt(m,maxLength,l,fileName)
        else:
            startDate = datetime.datetime(2020, 8, 15 ,12,00) # hard coded but can change
            createWithTimestamp(m,maxLength,l,startDate,fileName)
    elif create_extend =="extend": #extend the previous one
        m=int(arguments[2])
        maxLength=int(arguments[3])
        fileName=arguments[4]
        create_additional_log(m,maxLength,fileName)
    
    
   