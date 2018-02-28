"""
    This script downloads properties from AWS Parameter Store and saves them to a file locally.
"""
import sys
import os
import boto3

if len(sys.argv) != 3:
    print('Need to provide parameter path and outputfile arguments')
    print('Example: python GetPropertiesFromParameterStore.py "/configservice/" "config_override/application_override"')
    exit(1)
CONTEXT = sys.argv[1]
OUTPUTFILE = sys.argv[2]

SSM = boto3.client('ssm', region_name=os.environ.get('AWS_REGION', 'us-east-2'))
def get_parameters_by_path(path):
    """
    path is a string specifying a path. E.g. /configservice/
    :param path:
    :return properties:
    """
    response = SSM.get_parameters_by_path(Path=path, Recursive=False, WithDecryption=True, MaxResults=10)
    if response['ResponseMetadata']['HTTPStatusCode'] != 200:
        exit(1)
    yield response['Parameters']
    next_token = get_next_token(response)
    while next_token != '':
        response = SSM.get_parameters_by_path(
            Path=path,
            Recursive=False,
            WithDecryption=True,
            MaxResults=10,
            NextToken=next_token
        )
        if response['ResponseMetadata']['HTTPStatusCode'] != 200:
            exit(1)
        yield response['Parameters']
        next_token = get_next_token(response)



def get_next_token(response):
    if 'NextToken' in response:
        return response['NextToken']
    else:
        return ''

def strip_parameter_name(parameter_name):
    if parameter_name.startswith(CONTEXT):
        return parameter_name[len(CONTEXT):]
    #return parameter_name;

def get_parameter_touples_with_stripped_context_prefixes(parameters):
    for parameter in parameters:
        yield (strip_parameter_name(parameter['Name']), parameter['Value'])


parameters = []
for tmp in get_parameters_by_path(CONTEXT):
    parameters += tmp
print(parameters)

properties = get_parameter_touples_with_stripped_context_prefixes(parameters)

property_names = []
with open(OUTPUTFILE, 'w') as f:
    for prop in properties:
        f.write(prop[0] + '=' + prop[1] + '\n')
        property_names.append(prop[0])

print('Wrote the following properties to ' + OUTPUTFILE + ': ' + ", ".join([x for x in property_names]))

exit(0)