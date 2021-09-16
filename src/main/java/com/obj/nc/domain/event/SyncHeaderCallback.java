/*
 *   Copyright (C) 2021 the original author or authors.
 *
 *   This file is part of Notiflow
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.obj.nc.domain.event;

import org.springframework.core.annotation.Order;
import org.springframework.data.relational.core.mapping.event.AfterLoadCallback;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class SyncHeaderCallback implements AfterLoadCallback<GenericEvent> {
	
	@Override
	public GenericEvent onAfterLoad(GenericEvent event) {
		event.syncHeaderFields();
		return event;
	}
}
