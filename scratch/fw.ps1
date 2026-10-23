param (
    [string]$path,
    [string]$b64Code
)
python scratch/writer.py $path $b64Code
