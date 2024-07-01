filename = 'data/pi_digits.text'
pi_String = ''
with open (filename) as f:
for line in f:
pi_String  += line.strip()
print(pi_String)
print(len(filename))
