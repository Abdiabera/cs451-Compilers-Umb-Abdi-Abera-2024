
class SocialMedia:
    """
    base class of social media also inherited
     by class twitter, facebook and linkedIn
    
    """
    def __init__(self, nameofmedia, url):
        """
        Constructor for social media
        """
        self.nameofmedia=nameofmedia
        self.url = url

    def post_message(self, msg):
        """
        This function will be overriden by derived classes
       
        """
        print("Posting not implemented in base class")


class Twitter(SocialMedia):
    """
    Twitter class deriving from SocialMedia class
    """
    def __init__(self):
        """
        constructor for twitter class
        that provides the super class constructors with the
        parameters twitter and twitter.com
        """
        super(Twitter, self).__init__('Twitter', 'twitter.com')

    def post_message(self, msg):
        """
        post_message function being overridden from base class
        If the length of the message is greater than 80 characters
        then display too long message, else then it returns none
        """
        if len(msg) > 80:
            print('Message is too long')
        else:
            print(f'Posting message to twitter: {msg}')


class Facebook(SocialMedia):

    """
    Facebook class derived from SocialMedia class
    """
    def __init__(self):

        """
        constructor for facebook class
        that provides the super class constructors with the
        parameters facebook and facebook.com
        """
        super(Facebook, self).__init__('Facebook', 'facebook.com')

    def post_message(self, msg):

        """
        post_message function being overridden from base class
        displays the message for the facebook class
        """
        print(f'Posting messages to all your friends on facebook: {msg}')


class LinkedIn(SocialMedia):
    """
    LinkedIn class derived from SocialMedia class
    """
    def __init__(self):


        """
        Constructor for LinkedIn class
        that provides the super class constructor with parameters
        LinkedIn and linkedIn.com
        """
        super(LinkedIn, self).__init__('LinkedIn', 'linkedin.com')

    def post_message(self, msg):
        """
        post_message function being overridden from base class
        displays the message for the linkedIn class
       
        """
        print(f'Posting messages to all your colleagues on LinkedIn: {msg}')
