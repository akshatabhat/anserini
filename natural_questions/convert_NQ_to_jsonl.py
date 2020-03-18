import json
import pandas as pd
import argparse
import os 

def convert_NQ(args):
    print('Converting NQ...')
    file_index = 0
    document_df = pd.read_hdf(args.collection_path, key="elements")
    i = 0
    index_table = {}
    for index, row in document_df.iterrows():
        doc_id = row["doc_id"]
        content = row["element"]
        element_id=row["e_id"]
        index_table[i] = {"doc_id":doc_id, "element_id":element_id, "content": content}
        if i % args.max_docs_per_file == 0:
            if i > 0:
                output_jsonl_file.close()
            output_path = os.path.join(args.output_folder, 'docs{:02d}.json'.format(file_index))
            output_jsonl_file = open(output_path, 'w', encoding='utf-8', newline='\n')
            file_index += 1
        output_dict = {'id': str(i), 'contents': content}
        output_jsonl_file.write(json.dumps(output_dict) + '\n')

        if i % 100000 == 0:
            print('Converted {} docs in {} files'.format(i, file_index))
        i += 1
    output_jsonl_file.close()
    with open(args.index_table+'/index_table.json', 'w') as fp:
        json.dump(index_table, fp)
if __name__=='__main__':
    parser = argparse.ArgumentParser(description='''Converts processedx NQ to Anserini jsonl files.''')
    parser.add_argument('--collection_path', required=True, help='NQ .h5 file')
    parser.add_argument('--output_folder', required=True, help='output filee')
    parser.add_argument('--index_table', required=True, help='index table filee')
    parser.add_argument('--max_docs_per_file', default=1000000, type=int, help='maximum number of documents in each jsonl file.')

    args = parser.parse_args()

    if not os.path.exists(args.output_folder):
        os.makedirs(args.output_folder)

    convert_NQ(args)
    print('Done!')
    

