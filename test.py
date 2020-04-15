from pyserini.search import pysearch

searcher = pysearch.SimpleSearcher('natural_questions/lucene-index-msmarco/')
hits = searcher.search('who is the announcer on americas got talent?')

# Print the first 10 hits:
for i in range(0, 10):
    print(f'{i+1} {hits[i].docid} {hits[i].score}')

# Grab the actual text:
hits[0].content