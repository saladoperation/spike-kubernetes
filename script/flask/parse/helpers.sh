#!/usr/bin/env bash
cd python
export PYTHONPATH=$(pwd)
source activate spike-kubernetes
python spike_kubernetes/helpers.py
