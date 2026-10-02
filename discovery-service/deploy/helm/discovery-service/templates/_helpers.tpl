{{/*
Expand the name of the chart.
*/}}
{{- define "discovery-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "discovery-service.fullname" -}}
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
{{- define "discovery-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels.
*/}}
{{- define "discovery-service.labels" -}}
helm.sh/chart: {{ include "discovery-service.chart" . }}
{{ include "discovery-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: community-platform
app.kubernetes.io/component: infrastructure
{{- end }}

{{/*
Selector labels.
*/}}
{{- define "discovery-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "discovery-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use.
*/}}
{{- define "discovery-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "discovery-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Generate Eureka peer URLs for StatefulSet replicas.
*/}}
{{- define "discovery-service.peerUrls" -}}
{{- $fullname := include "discovery-service.fullname" . -}}
{{- $releaseNamespace := .Release.Namespace -}}
{{- $port := .Values.service.port -}}
{{- $replicas := int .Values.replicaCount -}}
{{- $urls := list -}}
{{- range $i := until $replicas -}}
{{- $urls = append $urls (printf "http://${EUREKA_USERNAME}:${EUREKA_PASSWORD}@%s-%d.%s-headless.%s.svc.cluster.local:%d/eureka/" $fullname $i $fullname $releaseNamespace $port) -}}
{{- end -}}
{{- join "," $urls -}}
{{- end }}
