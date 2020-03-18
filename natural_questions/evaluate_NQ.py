from pyserini.search import pysearch
import json
import pandas as pd
import argparse

def evaluate(examples_df, index_table, k):
    print("Begin evaluation")
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
        hits = searcher.search(question, k=k)
        for i in range(0, k):
            index = hits[i].docid
            #print(i, ":", index_table[index]["content"])
            #print("score : ", hits[i].docid)
            if index_table[index]["doc_id"] == actual_doc_id and index_table[index]["element_id"] == actual_element_id:
                correct +=1
                break
    print("Total number of examples : ", num_of_examples)
    recall = correct/num_of_examples
    print("Proportion of correct retrieval : %f" % (recall))       


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='''Converts processedx NQ to Anserini jsonl files.''')
    parser.add_argument('--input_dir', required=True, help='path to input directory')
    parser.add_argument('--k', required=True, type=int, help='number of documents to be returned by search')
    args = parser.parse_args()

    examples_df = pd.read_hdf(args.input_dir + "/NQ.h5", key="examples")
    with open(args.input_dir + '/index_table.json') as f:
        index_table = json.load(f)

    searcher = pysearch.SimpleSearcher(args.input_dir + '/lucene-index-msmarco/')

    evaluate(examples_df, index_table, args.k)