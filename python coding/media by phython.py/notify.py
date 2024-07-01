

from media import Twitter, Facebook, LinkedIn
#to excute some code only if the file is running directly
if __name__ == '__main__':
    twitter = Twitter()
    facebook = Facebook()
    linkedIn = LinkedIn()

    long_message = "akkam jirta fayakedha bayyen siyade. galatoomi " \
                   "waaqanikoo si haa eebbisuu barsiisaakoo naa jiraadhu, galatoomaa" \
                   "thank you a lot professor "
#posting message under twitter 
    twitter.post_message(long_message)

    short_message = 'Hello'
    print(short_message)

#posting messages under twitter, facebook, linkedIn checking weather its long or short
    twitter.post_message(short_message)
    facebook.post_message(short_message)
    linkedIn.post_message(short_message)