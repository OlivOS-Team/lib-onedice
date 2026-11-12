cd ..
python setup.py sdist bdist_wheel
twine upload -r pypi dist/*
cd script
