#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed Aug 12 20:41:37 2020

@author: mavroudo
"""
import sys
import random

def read_txt(logfile):
    data=[]
    with open(logfile,"r") as f:
        for l in f:
            for ev in l.replace("\n","").split(","):
                data+=[ev]
    return list(set(data))

def read_with_timestamps(logfile):
    data=[]
    with open(logfile,"r") as f:
        for l in f:
            for ev in l.replace("\n","").split(","):
                data+=[ev.split("/delab/")[0]]
    return list(set(data))

def create_queries(queryFile,num_queries,max_length_query,unique_events):
    with open(queryFile,"w") as f:
        for _ in range(num_queries):
            events=random.choices(unique_events,k=random.randint(1,max_length_query))
            f.write(",".join(events)+"\n")
            
            

if __name__ == "__main__":
    arguments=sys.argv
    logfile=arguments[1]
    num_queries=int(arguments[2])
    max_length_query=int(arguments[3])
    if logfile.split(".")[1]=="txt":
        create_queries(logfile.split(".")[0]+".queries",num_queries,max_length_query,read_txt(logfile))
    else:
        create_queries(logfile.split(".")[0]+".queries",num_queries,max_length_query,read_with_timestamps(logfile))
    
    