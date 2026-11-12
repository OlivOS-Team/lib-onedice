# -*- encoding: utf-8 -*-
'''
   ____  _   ____________  ________________
  / __ \/ | / / ____/ __ \/  _/ ____/ ____/
 / / / /  |/ / __/ / / / // // /   / __/   
/ /_/ / /|  / /___/ /_/ // // /___/ /___   
\____/_/ |_/_____/_____/___/\____/_____/   

@File      :   setup.py
@Author    :   lunzhiPenxil仑质
@Contact   :   lunzhipenxil@gmail.com
@License   :   AGPL
@Copyright :   (C) 2020-2021, OlivOS-Team
@Desc      :   None
'''
import setuptools

with open("README.md", "r", encoding="utf-8") as fh:
    long_description = fh.read()

setuptools.setup(
    name="onedice",
    version="1.0.7",
    author="lunzhiPenxil",
    author_email="lunzhiPenxil@gmail.com",
    description="OneDice offical standard library for Python",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/OlivOS-Team/lib-onedice",
    packages=setuptools.find_packages(),
    install_requires=[],
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: GNU Affero General Public License v3",
        "Operating System :: OS Independent",
    ],
)