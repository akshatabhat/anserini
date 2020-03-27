from pyserini.search import pysearch
import json
import pandas as pd
import argparse

def evaluate1(examples_df, args):
    with open(args.input_dir + '/index_table_doc.json') as f:
        index_table = json.load(f)
    k1 = args.k1
    searcher = pysearch.SimpleSearcher(args.input_dir +'/lucene-index-nq-doc')
    print("Begin evaluation 1")
    correct = 0
    num_of_examples = 0
    for index, example in examples_df.iterrows():
        num_of_examples += 1
        question = example["query"]
        actual_doc_id = example["doc_id"]
        #print("******************** Example id : ", num_of_examples, "***************")
        #print("question : ", question)
        #print("context : ")
        # Search using anserini
        hits = searcher.search(question, k=k1)
        for i in range(0, k1):
            index = hits[i].docid
            #print(hits[i].content)
            #print(i, ":", index_table[index]["content"])
            #print("score : ", hits[i].docid)
            if index_table[index]["doc_id"] == actual_doc_id:
                correct +=1
                break
    print("Total number of examples : ", num_of_examples)
    recall = correct/num_of_examples
    print("Proportion of correct retrieval : %f" % (recall))  

def evaluate2(examples_df, args):
    with open(args.input_dir + '/index_table_ele.json') as f:
        index_table = json.load(f)
    k2 = args.k2
    searcher = pysearch.SimpleSearcher(args.input_dir +'/lucene-index-nq-ele')
    print("Begin evaluation 2")
    correct = 0
    num_of_examples = 0
    for index, example in examples_df.iterrows():
        num_of_examples += 1
        question = example["query"]
        actual_element_id = example["context_id"]
        actual_doc_id = example["doc_id"]
        #print("******************** Example id : ", num_of_examples, "***************")
        #print("question : ", question)
        #print("context : ")
        # Search using anserini
        hits = searcher.search(question, k=k2)
        for i in range(0, k2):
            index = hits[i].docid
            #print(hits[i].content)
            #print(i, ":", index_table[index]["content"])
            #print("score : ", hits[i].docid)
            if index_table[index]["doc_id"] == actual_doc_id and index_table[index]["element_id"] == actual_element_id:
                 correct +=1
                 break
    print("Total number of examples : ", num_of_examples)
    recall = correct/num_of_examples
    print("Proportion of correct retrieval : %f" % (recall))       

def evaluate3(examples_df, args):
    # TODO: This doesn't make sense !!!
    with open(args.input_dir + '/index_table_doc.json') as f:
        index_table_doc = json.load(f)
    with open(args.input_dir + '/index_table_ele.json') as f:
        index_table_ele = json.load(f)
    k1 = args.k1
    k2 = args.k2
    searcher1 = pysearch.SimpleSearcher(args.input_dir +'/lucene-index-nq-doc')
    searcher2 = pysearch.SimpleSearcher(args.input_dir +'/lucene-index-nq-ele')
    print("Begin evaluation 3")
    correct = 0
    num_of_examples = 0
    for index, example in examples_df.iterrows():
        num_of_examples += 1
        question = example["query"]
        actual_element_id = example["context_id"]
        actual_doc_id = example["doc_id"]

        doc_hits = searcher1.search(question, k=k1)
        ele_hits = searcher2.search(question, k=k2)
        doc_ids = []
        
        for i in range(0, k1):
            index = doc_hits[i].docid
            doc_ids.append(index_table_doc[index]["doc_id"])
        for i in range(0, k2):
            index = ele_hits[i].docid
            if index_table_ele[index]["doc_id"] in doc_ids and index_table_ele[index]["doc_id"] == actual_doc_id and index_table_ele[index]["element_id"] == actual_element_id:
                correct +=1
                break

    print("Total number of examples : ", num_of_examples)
    recall = correct/num_of_examples
    print("Proportion of correct retrieval : %f" % (recall))  

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='''Converts processedx NQ to Anserini jsonl files.''')
    parser.add_argument('--input_dir', required=True, help='path to input directory')
    parser.add_argument('--k1', required=True, type=int, help='number of documents to be returned by search')
    parser.add_argument('--k2', required=True, type=int, help='number of documents to be returned by search')
    args = parser.parse_args()

    examples_df = pd.read_hdf(args.input_dir + "/NQ.h5", key="examples")
    evaluate1(examples_df, args)
    evaluate2(examples_df, args)
    evaluate3(examples_df, args)