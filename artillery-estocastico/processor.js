'use strict';

const fs = require('fs');

const dataset = JSON.parse(
  fs.readFileSync('./dataset.json', 'utf8')
);

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function poissonRandom(lambda) {
  const L = Math.exp(-lambda);
  let p = 1.0;
  let k = 0;

  do {
    k++;
    p *= Math.random();
  } while (p > L);

  return k - 1;
}

module.exports = {

  initPoisson: function (context, events, done) {

    context.vars.inicio = Date.now();
    context.vars.duracion = 60000;
    context.vars.totalObjetivo = 800;
    context.vars.totalEnviadas = 0;
    context.vars.oleadaRestante = 0;
    context.vars.forzarFinal = false;

    return done();
  },

  configurarOleada: function (context, events, done) {

    const ahora = Date.now();

    if (!context.vars.forzarFinal &&
        ahora - context.vars.inicio >= context.vars.duracion) {

      const faltantes = context.vars.totalObjetivo - context.vars.totalEnviadas;

      if (faltantes > 0) {
        context.vars.oleadaRestante = faltantes;
        context.vars.forzarFinal = true;
      } else {
        return done(new Error('STOP'));
      }
    }

    if (context.vars.totalEnviadas >= context.vars.totalObjetivo) {
      return done(new Error('STOP'));
    }

    if (!context.vars.forzarFinal && context.vars.oleadaRestante === 0) {
      const restante = context.vars.totalObjetivo - context.vars.totalEnviadas;
      const tamOleada = randomInt(1, Math.min( context.vars.totalObjetivo , restante));
      context.vars.oleadaRestante = tamOleada;
    }

    const data = dataset[randomInt(0, dataset.length - 1)];

    context.vars.tipo = data.tipo;
    context.vars.cantidad = data.cantidad;
    context.vars.usuario = data.usuario;

    context.vars.totalEnviadas++;
    context.vars.oleadaRestante--;

    if (context.vars.forzarFinal && context.vars.oleadaRestante === 0) {
      return done(new Error('STOP'));
    }

    const intervalo = context.vars.forzarFinal
      ? 0
      : poissonRandom(2) * randomInt(1, 50);

    setTimeout(() => done(), intervalo);
  }

};
