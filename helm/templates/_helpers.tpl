{{/*
Expand the name of the chart.
*/}}
{{- define "fintech-analytics.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "fintech-analytics.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "fintech-analytics.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "fintech-analytics.labels" -}}
helm.sh/chart: {{ include "fintech-analytics.chart" . }}
{{ include "fintech-analytics.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "fintech-analytics.selectorLabels" -}}
app.kubernetes.io/name: {{ include "fintech-analytics.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "fintech-analytics.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "fintech-analytics.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Get database connection URL
*/}}
{{- define "fintech-analytics.databaseUrl" -}}
{{- if .Values.database.external.enabled }}
{{- printf "jdbc:postgresql://%s:%d/%s" .Values.database.external.host .Values.database.external.port .Values.database.name }}
{{- else }}
{{- printf "jdbc:postgresql://%s-postgresql:5432/%s" .Release.Name .Values.database.name }}
{{- end }}
{{- end }}

{{/*
Get environment variables
*/}}
{{- define "fintech-analytics.envVars" -}}
- name: SPRING_DATASOURCE_URL
  value: {{ include "fintech-analytics.databaseUrl" . }}
- name: SPRING_DATASOURCE_USERNAME
  value: {{ .Values.database.username | quote }}
- name: SPRING_DATASOURCE_PASSWORD
  {{- if .Values.database.passwordFromSecret }}
  valueFrom:
    secretKeyRef:
      name: {{ .Values.database.passwordFromSecret.name }}
      key: {{ .Values.database.passwordFromSecret.key }}
  {{- else }}
  value: {{ .Values.database.password | quote }}
  {{- end }}
{{- range .Values.app.env }}
- name: {{ .name }}
  value: {{ .value | quote }}
{{- end }}
{{- if .Values.database.ssl }}
- name: SPRING_DATASOURCE_HIKARI_DATA_SOURCE_PROPERTIES_SSL_MODE
  value: {{ .Values.database.sslMode | default "require" | quote }}
{{- end }}
{{- if .Values.cache.enabled }}
- name: SPRING_REDIS_HOST
  value: {{ .Values.cache.host | quote }}
- name: SPRING_REDIS_PORT
  value: {{ .Values.cache.port | quote }}
{{- if .Values.cache.password }}
- name: SPRING_REDIS_PASSWORD
  value: {{ .Values.cache.password | quote }}
{{- end }}
{{- end }}
{{- end }}