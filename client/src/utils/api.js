import React from 'react';
import axios from 'axios';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import history from './history';

const configs = {
  withCredentials: true
};

const handleError = err => {
  let error = {
    status: (err.response) ? err.response.status : '',
    message: (err.response) ? err.response.data.message : 'Internal Server Error',
    stacktrace: (err.response && err.response.data && err.response.data._embedded
        && err.response.data._embedded.stacktrace && err.response.data._embedded.stacktrace.message) ?
        err.response.data._embedded.stacktrace.message :
        null
  };

  if (err.response && err.response.status < 500) {
    toast.warn(error.message);

    return error;
  } else {
    let message = React.createElement('h4', null, error.message);

    if (error.stacktrace) {
      let detailedReactHTMLElement = React.createElement('pre', null, error.stacktrace);
       message = React.createElement('div', null, message, detailedReactHTMLElement);
    }

    toast.error(message, {
      autoClose: false,
      className: 'internal-error'
    });

    return error;
  }
};

export const handleCatch = err => {
  if (err.status === 401) {
    history.replace('/ui/:login');
    this.setState({ clusterId: ':login' });

  }
};

export const get = url =>
  new Promise((resolve, reject) => {
    axios
      .get(url, configs)
      .then(res => {
        resolve(res);
      })
      .catch(err => {

        reject(handleError(err));
      });
  });

export const put = (url, body) =>
  new Promise((resolve, reject) => {
    axios
      .put(url, body, configs)
      .then(res => {
        resolve(res);
      })
      .catch(err => {
        reject(handleError(err));
      });
  });

export const post = (url, body) =>
  new Promise((resolve, reject) => {
    axios
      .post(url, body, configs)
      .then(res => {
        resolve(res);
      })
      .catch(err => {
        reject(handleError(err));
      });
  });

export const remove = (url, body) =>
  new Promise((resolve, reject) => {
    axios
      .delete(url, { ...configs, data: body })
      .then(res => {
        resolve(res);
      })
      .catch(err => {
        reject(handleError(err));
      });
  });

export const login = (url, body) => {
  const requestOptions = {
    method: 'POST',
    redirect: 'manual',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  };
  return fetch(url, requestOptions);
};

export const logout = url => {
  const requestOptions = {
    method: 'GET',
    redirect: 'manual'
  };

  new Promise((resolve, reject) => {
    fetch(url, requestOptions)
      .then(response => {
        resolve(response);
      })
      .catch(function(err) {
        reject(handleError(err));
      });
  });
};

export default { get, put, post, remove, login, logout, handleCatch };
