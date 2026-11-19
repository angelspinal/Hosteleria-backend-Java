'use strict';

const $ = (id) => document.getElementById(id);
const API = document.body.dataset.apiBase;

const panel = $('panelHuesped');
const formulario = $('formHuesped');
const botonGuardar = $('btnGuardar');

const campos = [
    'nombre',
    'apellido',
    'documento',
    'telefono',
    'correo'
];

let huespedes = [];
let idEdicion = null;

document.querySelector('[data-menu="huespedes"]')
    ?.classList.add('activo');

function mostrarMensaje(texto, error = false) {
    const elemento = $('mensaje');

    elemento.textContent = texto;
    elemento.className = `mensaje ${error ? 'error' : 'exito'}`;
    elemento.hidden = false;
}

function mostrarErrorFormulario(texto) {
    $('errorFormulario').textContent = texto;
    $('errorFormulario').hidden = false;
}

async function peticion(ruta, opciones = {}) {
    const respuesta = await fetch(`${API}${ruta}`, opciones);
    const texto = await respuesta.text();

    let datos = texto;

    if (texto) {
        try {
            datos = JSON.parse(texto);
        } catch {
            // Algunos errores de la API llegan como texto.
        }
    }

    if (!respuesta.ok) {
        let mensaje =
            `No se pudo completar la operación (${respuesta.status}).`;

        if (typeof datos === 'string' && datos.trim()) {
            if (!datos.trim().startsWith('<')) {
                mensaje = datos;
            }
        } else if (datos && typeof datos === 'object') {
            if (typeof datos.detail === 'string') {
                mensaje = datos.detail;
            } else if (typeof datos.message === 'string') {
                mensaje = datos.message;
            } else {
                const mensajes = Object.values(datos)
                    .filter((valor) => typeof valor === 'string');

                if (mensajes.length) {
                    mensaje = mensajes.join(' · ');
                }
            }
        }

        throw new Error(mensaje);
    }

    return datos;
}

function celda(fila, texto) {
    const elemento = document.createElement('td');

    elemento.textContent = texto ?? '—';
    fila.appendChild(elemento);

    return elemento;
}

function renderizarTabla() {
    const busqueda = $('buscar').value
        .trim()
        .toLocaleLowerCase('es');

    const filtrados = huespedes.filter((huesped) => {
        const persona = huesped.persona;

        const texto = [
            persona?.nombre,
            persona?.apellido,
            persona?.documento
        ].join(' ').toLocaleLowerCase('es');

        return texto.includes(busqueda);
    });

    const cuerpo = $('tablaHuespedes');
    cuerpo.replaceChildren();

    $('totalHuespedes').textContent = huespedes.length;

    $('cantidadResultados').textContent =
        `${filtrados.length} de ${huespedes.length} huéspedes`;

    if (!filtrados.length) {
        const fila = document.createElement('tr');

        const elemento = celda(
            fila,
            huespedes.length
                ? 'No hay resultados para esta búsqueda.'
                : 'Todavía no hay huéspedes registrados.'
        );

        elemento.colSpan = 6;
        elemento.className = 'tabla-vacia';

        cuerpo.appendChild(fila);
        return;
    }

    filtrados.forEach((huesped) => {
        const persona = huesped.persona;
        const fila = document.createElement('tr');

        celda(fila, huesped.id);

        celda(
            fila,
            persona
                ? `${persona.nombre} ${persona.apellido}`
                : 'Sin datos personales'
        );

        celda(fila, persona?.documento);
        celda(fila, persona?.telefono);
        celda(fila, persona?.correo);

        const acciones = celda(fila, '');
        const boton = document.createElement('button');

        boton.type = 'button';
        boton.className = 'accion';
        boton.textContent = 'Editar';
        boton.disabled = !persona;

        boton.addEventListener(
            'click',
            () => abrirFormulario(huesped)
        );

        acciones.appendChild(boton);
        cuerpo.appendChild(fila);
    });
}

async function cargarHuespedes() {
    const datos = await peticion('/listar');

    if (!Array.isArray(datos)) {
        throw new Error('La API no devolvió una lista de huéspedes.');
    }

    huespedes = datos;
    renderizarTabla();
}

function abrirFormulario(huesped = null) {
    formulario.reset();
    $('errorFormulario').hidden = true;

    idEdicion = huesped?.id ?? null;

    $('tituloPanel').textContent =
        idEdicion !== null ? 'Editar huésped' : 'Nuevo huésped';

    botonGuardar.textContent =
        idEdicion !== null ? 'Guardar cambios' : 'Registrar huésped';

    if (huesped) {
        campos.forEach((campo) => {
            $(campo).value = huesped.persona?.[campo] ?? '';
        });
    }

    panel.showModal();
    $('nombre').focus();
}

function cerrarFormulario() {
    if (!botonGuardar.disabled) {
        panel.close();
    }
}

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    $('errorFormulario').hidden = true;

    const datos = {};

    for (const campo of campos) {
        datos[campo] = $(campo).value.trim();

        if (!datos[campo]) {
            mostrarErrorFormulario(
                'Completa todos los campos sin dejar solo espacios.'
            );

            $(campo).focus();
            return;
        }
    }

    const editando = idEdicion !== null;

    const ruta = editando
        ? `/${idEdicion}/datos-personales`
        : '/registrar';

    const textoBoton = botonGuardar.textContent;

    botonGuardar.disabled = true;
    botonGuardar.textContent = 'Guardando...';

    // Bloquea los campos mientras se procesa el registro.
    campos.forEach((campo) => {
        $(campo).disabled = true;
    });

    try {
        await peticion(ruta, {
            method: editando ? 'PUT' : 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(datos)
        });

        panel.close();

        mostrarMensaje(
            editando
                ? 'Datos del huésped actualizados correctamente.'
                : 'Huésped registrado correctamente.'
        );

        try {
            await cargarHuespedes();
        } catch {
            mostrarMensaje(
                'Los datos se guardaron, pero no se pudo actualizar '
                + 'el listado. Recarga la página.',
                true
            );
        }
    } catch (error) {
        mostrarErrorFormulario(error.message);
    } finally {
        botonGuardar.disabled = false;
        botonGuardar.textContent = textoBoton;

        campos.forEach((campo) => {
            $(campo).disabled = false;
        });
    }
});

$('btnNuevo').addEventListener('click', () => abrirFormulario());
$('btnCerrar').addEventListener('click', cerrarFormulario);
$('btnCancelar').addEventListener('click', cerrarFormulario);
$('buscar').addEventListener('input', renderizarTabla);

panel.addEventListener('cancel', (evento) => {
    if (botonGuardar.disabled) {
        evento.preventDefault();
    }
});

cargarHuespedes().catch((error) => {
    const cuerpo = $('tablaHuespedes');
    cuerpo.replaceChildren();

    const fila = document.createElement('tr');
    const elemento = celda(
        fila,
        'No se pudieron cargar los huéspedes.'
    );

    elemento.colSpan = 6;
    elemento.className = 'tabla-vacia';

    cuerpo.appendChild(fila);
    mostrarMensaje(error.message, true);
});