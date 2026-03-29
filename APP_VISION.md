# Ewe-Shop App — Visión y flujo de pedidos

Esta app es para **tiendas y restaurantes**. El negocio inicia sesión con teléfono + OTP (solo si ya está registrado en el panel web) y recibe los pedidos que hacen los clientes desde la app Ewe (cliente).

---

## Flujo del pedido (vista tienda)

1. **Cliente hace el pedido** (app Ewe) → el backend crea la orden con `shopId`, estado `PENDING`.
2. **La tienda ve el pedido** en la app Ewe-Shop en la lista de “Pedidos” (solo pedidos de su `shopId`).
3. **La tienda puede**:
   - **Aceptar** → estado pasa a `CONFIRMED` (estamos preparando).
   - **Rechazar** → estado pasa a `CANCELLED`.
4. Cuando la tienda confirma, más adelante el **admin asigna un repartidor** y el estado pasa a `ASSIGNED` → `IN_PROGRESS` → `DELIVERED`.

---

## Cómo imagino la app (pantallas)

### 1. **Inicio (Home)**

- Resumen rápido: “X pedidos pendientes hoy”.
- Acceso directo a la lista de pedidos.
- (Opcional más adelante: menú, horarios, estadísticas.)

### 2. **Pedidos (lista)**

- Lista de pedidos de la tienda, ordenados por fecha (más recientes primero).
- Filtros por estado: Pendientes, Confirmados, Asignados, En camino, Entregados, Cancelados.
- Cada ítem muestra: hora, total, dirección de entrega, estado, y un botón para ver detalle.

### 3. **Detalle del pedido**

- **Productos**: nombre, cantidad, precio.
- **Total** del pedido.
- **Dirección de entrega** (y opcionalmente link a Maps).
- **Estado** actual.
- **Acciones** (solo si está `PENDING`):
  - **Aceptar** → llama a `PATCH /api/shop/orders/:id/accept`.
  - **Rechazar** → llama a `PATCH /api/shop/orders/:id/reject`.
- Si ya está confirmado o asignado, mostrar quién es el repartidor (cuando el backend lo envíe).

### 4. **Perfil / Ajustes**

- Nombre de la tienda, teléfono (solo lectura o editable si hay API).
- Cerrar sesión.

---

## API para la tienda (ya implementada en el backend)

Todas las rutas van bajo `/api/shop/` y requieren **JWT de tienda** (el que se obtiene con `auth/shop/verify-otp`).

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET    | `/api/shop/orders` | Lista de pedidos de esta tienda. Query opcional: `?status=PENDING` (o CONFIRMED, ASSIGNED, etc.). |
| PATCH  | `/api/shop/orders/:id/accept` | Aceptar pedido (PENDING → CONFIRMED). |
| PATCH  | `/api/shop/orders/:id/reject` | Rechazar pedido (PENDING → CANCELLED). |

Cada pedido en la lista incluye: `id`, `status`, `total`, `deliveryAddress`, `createdAt`, `items` (producto, cantidad, precio), `deliveryMan` (si está asignado).

---

## Estados del pedido (OrderStatus)

- **PENDING** — Recién creado; la tienda debe aceptar o rechazar.
- **CONFIRMED** — Tienda aceptó; se está preparando (o listo para que asignen repartidor).
- **PREPARING** — Tienda está preparando / trabajando en el pedido.
- **READY_FOR_PICKUP** — Listo para recoger (repartidor puede asignarse).
- **ASSIGNED** — Admin asignó repartidor.
- **IN_PROGRESS** — Repartidor en camino.
- **DELIVERED** — Entregado.
- **CANCELLED** — Cancelado (por tienda o por otro motivo).

Desde la app Ewe-Shop: con pedido **Confirmado** → **Marcar en preparación** (PREPARING); con pedido **En preparación** → **Listo para recoger** (READY_FOR_PICKUP).

---

## Próximos pasos sugeridos en la app Android

1. **Pantalla de pedidos**  
   - Llamar a `GET /api/shop/orders` con el token de tienda.  
   - Mostrar lista (y opcionalmente filtro por estado).

2. **Pantalla de detalle**  
   - Al tocar un pedido, navegar a detalle con ítems, dirección, total.  
   - Botones Aceptar / Rechazar si `status === "PENDING"`.

3. **Home**  
   - Contador de pedidos pendientes (por ejemplo `status=PENDING`) y enlace a la lista.

4. **(Opcional)** Notificaciones push cuando llegue un pedido nuevo (backend + FCM más adelante).

Con esto la tienda puede **recibir** los pedidos, **ver el detalle** y **aceptar o rechazar** desde la app Ewe-Shop.
