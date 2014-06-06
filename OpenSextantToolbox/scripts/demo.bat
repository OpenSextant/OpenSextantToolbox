REM get the 
curl -X POST http://localhost:8182/opensextant/extract/
curl -X POST http://localhost:8182/opensextant/extract/geo/

curl -X POST http://localhost:8182/opensextant/extract/geo/csv -d "@../testdata/ace.txt"

curl -X POST http://localhost:8182/opensextant/extract/geo/csv -F "infile=@../testdata/ace.txt"
curl -X POST http://localhost:8182/opensextant/extract/geo/csv -F "infile=@../testdata/ace.htm;type=text/html"

curl -X POST "http://localhost:8182/opensextant/extract/geo/csv/url/http%3A%2F%2Femployeeshare.mitre.org%2Fd%2Fdlutz%2Ftransfer%2Fmobile_worker.pdf"

curl -X POST http://localhost:8182/opensextant/extract/geo/csv -d "We drove to Kabul, which is located at 1234N 01234W."
curl -X POST http://localhost:8182/opensextant/extract/geo/json -d "We drove to Kabul, which is located at 1234N 01234W."
curl -X POST http://localhost:8182/opensextant/extract/geo/xml -d "We drove to Kabul, which is located at 1234N 01234W."


curl -X GET http://localhost:8182/opensextant/lookup/json/Kabul
curl -X GET http://localhost:8182/opensextant/lookup/csv/Kabul
curl -X GET http://localhost:8182/opensextant/lookup/json/Kabul/AF
curl -X GET http://localhost:8182/opensextant/lookup/csv/Kabul/AF
curl -X GET http://localhost:8182/opensextant/lookup/csv/query/name:Ka*%20AND%20cc:AF