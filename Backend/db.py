from sqlalchemy import *
from sqlalchemy.ext.declarative import declarative_base

engine = create_engine('sqlite:///database.db', echo=False)

Base = declarative_base()

'''
Object models
'''
class ActivityLabel(Base):
    __tablename__ = 'ActivityLabel'

    Id = Column(Integer, primary_key=True)
    Name = Column(String, nullable=False, unique=true)

class BodyPosition(Base):
    __tablename__ = 'BodyPosition'

    Id = Column(Integer, primary_key=True)
    Name = Column(String, nullable=False, unique=true)

class DataSet(Base):
    __tablename__ = 'DataSet'

    Id = Column(Integer, primary_key=True)
    Activity_Label = Column(Integer, ForeignKey('ActivityLabel.Id'))
    Body_Position = Column(Integer, ForeignKey('BodyPosition.Id'))
    Time_Stamp = Column(Integer, nullable=False)
    Ax = Column(Float, nullable=False)
    Ay = Column(Float, nullable=False)
    Az = Column(Float, nullable=False)
    Gx = Column(Float, nullable=False)
    Gy = Column(Float, nullable=False)
    Gz = Column(Float, nullable=False)
    Ma = Column(Float, nullable=False)
    My = Column(Float, nullable=False)
    Mz = Column(Float, nullable=False)

'''
/Object models
'''

# Now these relation model will be created in the database
# Here, SQLAlchemy will use CREATE TABLE statements
Base.metadata.create_all(engine)

