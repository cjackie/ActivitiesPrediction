from sqlalchemy import *
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

# Connect engine
engine = create_engine('sqlite:///database.db', echo=False)

# This Base will be used for making mapped classes
Base = declarative_base()

# Create a session maker class
Session = sessionmaker(bind=engine)

'''
Object models
'''


class Label(Base):
    __tablename__ = 'ActivityLabel'

    id = Column(Integer, primary_key=True)
    name = Column(String, nullable=False, unique=true)


class Position(Base):
    __tablename__ = 'BodyPosition'

    id = Column(Integer, primary_key=True)
    name = Column(String, nullable=False, unique=true)


class DataSet(Base):
    __tablename__ = 'DataSet'

    id = Column(Integer, primary_key=True)
    label_id = Column(Integer, ForeignKey('ActivityLabel.id'))
    position_id = Column(Integer, ForeignKey('BodyPosition.id'))
    timestamp = Column(Integer, nullable=False)
    ax = Column(Float, nullable=False)
    ay = Column(Float, nullable=False)
    az = Column(Float, nullable=False)
    gx = Column(Float, nullable=False)
    gy = Column(Float, nullable=False)
    gz = Column(Float, nullable=False)
    mx = Column(Float, nullable=False)
    my = Column(Float, nullable=False)
    mz = Column(Float, nullable=False)

'''
/Object models
'''

# Now these relation model will be created in the database
# Here, SQLAlchemy will use CREATE TABLE statements
Base.metadata.create_all(engine)

'''
Data class
'''
class Data:
    def __init__(self, **kwargs):
        keys = {'label', 'position', 'timestamp',
                'ax', 'ay', 'az',
                'gx', 'gy', 'gz',
                'mx', 'my', 'mz'}
        self.data = {k: kwargs[k] for k in keys}
        keys = {'label_id', 'position_id'}
        self.data.update({k: None for k in keys})

    def insert(self):
        session = Session()

        # get id of label and position
        # remember that it returns a tuple, not a single value
        self.data['label_id'] = session.query(Label.id).\
            filter(Label.name == self.data['label']).one_or_none()
        self.data['position_id'] = session.query(Position.id).\
            filter(Position.name == self.data['position']).one_or_none()

        # if label and position doesn't exist add
        if not self.data['label_id']:
            activity = Label(name=self.data['label'])
            session.add(activity)
            session.commit()
            self.data['label_id'] = activity.id
        else:
            # convert tuple
            self.data['label_id'] = self.data['label_id'][0]

        if not self.data['position_id']:
            position = Position(name=self.data['position'])
            session.add(position)
            session.commit()
            self.data['position_id'] = position.id
        else:
            # convert tuple
            self.data['position_id'] = self.data['position_id'][0]

        # add new dataset
        self.data.pop('label')
        self.data.pop('position')
        new_data = DataSet(**self.data)
        session.add(new_data)
        session.commit()



'''
Main function
'''
if __name__ == '__main__':
    dic = {'label': "new", 'position': "new", 'timestamp': 123,
           'ax': 1.0, 'ay': 1.1, 'az': 1.2,
           'gx': 2.0, 'gy': 2.1, 'gz': 2.2,
           'mx': 3.0, 'my': 3.1, 'mz': 3.2}

    new_data = Data(**dic)
    new_data.insert()
