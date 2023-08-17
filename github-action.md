## Github Action 설정
- Github Action를 공부하면서 테스트한 내용입니다.

### Tech
- Backend와 Frontend 분리 배포
- 순수 서버 배포(AWS S3 등 사용 X)
- Pricate Repository 테스트
- Spring Boot 3.1.2
- JDK 17
- React v18

##

### Github Action
GitHub Actions는 GitHub에서 제공하는 자동화 및 워크플로우 자동화 서비스입니다. 개발자들은 소프트웨어 개발 및 배포 프로세스를 자동화하고 개선하는 데 사용할 수 있습니다. 이를 통해 코드 변경 사항을 감지하고, 자동 테스트를 수행하며, 빌드 및 배포를 자동화하여 개발 및 협업 프로세스를 효율적으로 관리할 수 있습니다.
운영 및 개발 서버환경을 맞추어서 테스트를 할 수 있습니다. 내부적으로 미니 서버를 생성해서 작업한다고 생각하면 이해하기가 좀 더 쉬울것 같습니다. 

##

### Tip
Github Action 설정파일이 틀린곳은 없는지 구문 확인해 주는 사이트
- Github Action Checker : https://rhysd.github.io/actionlint/

##

### Code
- [Github Action 전체코드](https://github.com/PARKJINHOH/github-action/blob/develop/.github/workflows/gradle.yml)
- 아래 코드 설명은 Backend(Spring boot)에 대해서 설명합니다.
  - Frontend도 Backend와 비슷합니다.

<hr/>

### 1개의 job에는 여러개의 step이 있을 수 있으며, job은 여러개일 수 있습니다.
```
jobs:
    changes:
        runs-on: ubuntu-latest
        steps:
            ...
    build:
        runs-on: ubuntu-latest
        steps:
            ...
    deploy:
        runs-on: ubuntu-latest
        steps:
            ...
```

<hr/>

## Github Action Code 설명
### Github Action 이름
```
name: Spring & React
```

##

### 해당 브랜치에서 push, pull_request 이벤트가 일어 났을 때 jobs가 실행.
- 프로젝트 경로를 설정하고 싶다면 path 추가
```
on:
  push:
    paths:
        - 'spring-action/'
        - 'react-action/'
    branches: [ "develop" ]
  pull_request:
    branches: [ "develop" ]
```

##

### changes job에서는 변경을 감지합니다.
- `runs-on`은 실행 환경을 지정합니다. `ubuntu-20.04` , `windows-2022` , `macos-13` 등이 있으며 고정 버전을 사용할 수 있습니다.
[runs-on 설명서](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#choosing-github-hosted-runners)
- `actions/checkout@v3` 에서는 repository를 checkout합니다. 아래 변경을 감지하기 위해 필요합니다.
  - token은 해당 repository에 접근하기 위해 사용되었습니다. Private Repository에서는 필요했습니다.
- `dorny/paths-filter@v2` 에서는 with/filters에 위치한 경로에 변경을 감지합니다.
- `outputs`은 변수입니다. 아래 코드에서는 backend, frontend 2개의 변수가 선언되어 있으며 `steps.filter.outputs.backend(front)`에 대한 value값을 가지고 있습니다.
  - `dorny/paths-filter`에서는 변경이 있으면 `true`없으면`false`를 반환합니다.
```
  changes:
    runs-on: ubuntu-latest
    outputs:
      backend: ${{ steps.filter.outputs.backend }}
      frontend: ${{ steps.filter.outputs.frontend }}

    steps:
    - name: checkout
      uses: actions/checkout@v3
      with:
        ref: develop
        repository: PARKJINHOH/github-action
        token: ${{ secrets.TOKEN_GITHUB }}

    - name : Run Paths Filter
      uses: dorny/paths-filter@v2
      id: filter
      with:
        filters: |
          backend:
            - 'spring-action/**'
          frontend:
            - 'react-action/**'
```

##

### back Job에서는 gradle build를 담당합니다.
- `if: ${{ needs.changes.outputs.backend == 'true' }}` 위에서 선언했던 변수가 true라면 아래 가 실행됩니다.
  - `backend: - 'spring-action/**'`에 변경이 없다면 해당 job의 steps는 실행되지 않습니다.
- `env: working-directory: ./spring-action` env설정입니다. backend의 경로를 입력했습니다.
- `actions/setup-java@v3` build에 필요한 JDK를 설정합니다. 
- `actions/checkout@v3` 에서는 repository를 checkout합니다. gradle build하기 위해 필요합니다.
- `actions/cache@v3` gradle build를 매번 할 시 시간이 매우 오래 걸리기 때문에 cache를 통해 좀 더 시간과 자원을 절약할 수 있습니다.
- `run`에서는 script를 실행할 수 있습니다. `./gradlew clean bootJar`를 통해 clean후 jar파일을 생성합니다.
- `actions/upload-artifact@v3` job간에는 파일을 이동할 수 없습니다. 워크플로우내에서 생성된 파일이나 데이터를 저장하고 다른 워크플로우에서 공유하기 위해 사용됩니다. 
여기서는 build된 jar파일을 다른 job에 넘겨서 원격 서버에 전송하기위해 추가되었습니다.
  - path는 Full Path이며, 경로를 자세히 모르겠다면 `-name pwd`로 현재 경로를 확인할 수 있습니다.
  - name은 중요합니다. download할 때 해당 name으로 찾아서 download합니다.
```
backend:
    needs: changes
    if: ${{ needs.changes.outputs.backend == 'true' }}
    runs-on: ubuntu-latest
    env:  
      working-directory: ./spring-action

    steps:
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: checkout
      uses: actions/checkout@v3
      with:
        ref: develop
        repository: PARKJINHOH/github-action
        token: ${{ secrets.TOKEN_GITHUB }}

    - name: Cache Gradle
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Gradle wrapper & build
      working-directory: ${{ env.working-directory }}
      run: |
        chmod +x ./gradlew
        ./gradlew clean bootJar
        ls ./build/libs
        
    - name: pwd
      run: ls -al .
        
    - name: Upload artifact
      uses: actions/upload-artifact@v3
      with:
        name: jar-artifact
        path: ${{ env.working-directory }}/build/libs #Full Path
```

##

### backend-upload에서는 서버로 파일을 전송합니다. 
- `actions/download-artifact@v3`에서는 `actions/upload-artifact@v3`에서 업로드한 파일을 다운로드 합니다.
  - 파일을 다운로드 할 때는 name으로 찾습니다. upload의 name과 동일해야 합니다.
- `appleboy/scp-action`은 ubuntu의 scp를 이용해 파일을 전송합니다.
  - SSH_IP는 도메인 혹은 IP도 가능합니다.
  - key가 필요한 server일 경우 secrets에 등록해서 사용할 수 있습니다.
  - source는 전송 대상입니다. `*.jar`일 경우 jar파일 모두, `"./**"`일 경우 현재 폴더 전체 파일 입니다.
    - pwd를 통해 현재 위치를 확인하세요.
  - target은 서버에서 받을 경로 입니다.
```
  backend-upload:
    needs: backend
    runs-on: ubuntu-latest

    steps: 
    - name: Download artifact
      uses: actions/download-artifact@v3
      with:
        name: jar-artifact
        
    #- name: pwd
    #  run: ls -al .

    - name: copy file via ssh password
      uses: appleboy/scp-action@v0.1.4
      with:
        host: ${{ secrets.SSH_IP }}
        username: ubuntu
        password: ${{ secrets.SSH_PASSWORD }}
        port: 22
        key: ${{ secrets.SSH_RSA }}
        source: "*.jar"
        target: ${{ secrets.DIST_PATH_BACKEND }}/staging
```

### (Frontend) ssh 접속
※ `ssh-action`와 `scp-action`은 다릅니다. 철자에 주의해주세요.
- `appleboy/ssh-action`을 통해 ssh에 접속할 수 있습니다.
  - `script: |`를 통해 서버에 있는 sh를 실행할 수도 있으며 sh를 아래 코드로 만들어 실행할 수 있습니다.
```
    steps:
      - name: executing remote ssh commands using password
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.SSH_IP }}
          username: ubuntu
          password: ${{ secrets.SSH_PASSWORD }}
          port: 22
          key: ${{ secrets.SSH_RSA }}
          script: |
            cd ${{ secrets.DIST_PATH_FRONT }}
            #./2.deploy.sh
```

<hr/>

## 실행 결과 사진

#### 서버 Upload 결과
![스크린샷 2023-08-17 223028](https://github.com/PARKJINHOH/github-action/assets/24603994/e2c7aed9-6fd2-4de2-ad8b-a30a0c99bbef)
![스크린샷 2023-08-17 223120](https://github.com/PARKJINHOH/github-action/assets/24603994/616bf5e5-22d5-46e0-98a9-3258126fd856)

#### Github Action 실행 결과
### Backend 만 변경시
![스크린샷 2023-08-17 223147](https://github.com/PARKJINHOH/github-action/assets/24603994/da90214a-718f-49a5-a851-889b8faec0a7)
### Frontend 만 변경시
![스크린샷 2023-08-17 223156](https://github.com/PARKJINHOH/github-action/assets/24603994/bcdf8f17-585a-4d99-a4f0-78f85bfd1c77)


<hr/>

### 기타
- 테스트 코드삽입
- schduler 가능 (해당 job을 scheduler로 돌릴 수 있습니다.)