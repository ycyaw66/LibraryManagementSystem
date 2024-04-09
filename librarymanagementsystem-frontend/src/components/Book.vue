<template>
    <el-scrollbar height="100%" style="width: 100%; height: 100%; ">
        <!-- 标题 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">图书管理</div>  

        <!-- 查询框第一行 + 查询和添加按钮 -->
        <div style="margin-left: 20px; padding-top:3vh;">
            <el-input v-model="this.toQueryInfo.title" style="display:inline; margin-left: 20px;" placeholder="书名"></el-input>
            <el-input v-model="this.toQueryInfo.category" style="display:inline; margin-left: 20px;" placeholder="类别"></el-input>
            <el-input v-model="this.toQueryInfo.author" style="display:inline; margin-left: 20px;" placeholder="作者"></el-input>
            <el-input v-model="this.toQueryInfo.press" style="display:inline; margin-left: 20px;" placeholder="出版社"></el-input>

            <el-button style="margin-left: 20px;" type="primary" @click="QueryBooks">查询</el-button>

            <el-button @click="newBookInfo.category = '', newBookInfo.title = '', newBookInfo.author = '', newBookInfo.press = '', newBookInfo.publishYear = 2000, newBookInfo.price = 0, newBookInfo.stock = 0, newBookVisible = true" style="margin-left: 20px;" type="success">添加</el-button>

            <el-button @change="handleFileUpload" style="margin-left: 20px;" type="success" @click="newBookSetVisible = true">批量入库</el-button>
            
        </div>

        <!-- 查询框第二行 + 借书和还书按钮 -->
        <div style="margin-left: 20px; padding-top:1vh;">
            <el-input v-model="this.toQueryInfo.minPublishYear" style="display:inline; margin-left: 20px; margin-top: 20px;" placeholder="最小出版年份"></el-input> --
            <el-input v-model="this.toQueryInfo.maxPublishYear" style="display:inline; margin-left: 0; margin-top: 20px;" placeholder="最大出版年份"></el-input>
            <el-input v-model="this.toQueryInfo.minPrice" style="display:inline; margin-left: 20px; margin-top: 20px;" placeholder="最小价格"></el-input> --
            <el-input v-model="this.toQueryInfo.maxPrice" style="display:inline; margin-left: 0; margin-top: 20px;" placeholder="最大价格"></el-input>

            <el-button style="margin-left: 20px;" type="warning" @click="borrowReturnBook = '', borrowReturnCard = '', borrowBookVisible = true">借书</el-button>

            <el-button style="margin-left: 20px;" type="warning" @click="borrowReturnBook = '', borrowReturnCard = '', returnBookVisible = true">还书</el-button>
        </div>

        <!-- 新建图书对话框 -->
        <el-dialog v-model="newBookVisible" title="添加图书" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类别：
                <el-input v-model="newBookInfo.category" style="margin-left: 24pt; width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                书名：
                <el-input v-model="newBookInfo.title" style="margin-left: 24pt; width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                作者：
                <el-input v-model="newBookInfo.author" style="margin-left: 24pt; width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社：
                <el-input v-model="newBookInfo.press" style="margin-left: 12pt; width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版年份：
                <el-input-number v-model="newBookInfo.publishYear" style="width: 12.5vw;" :min="-5000" :step="1" step-strictly />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                价格：
                <el-input-number v-model="newBookInfo.price" style="margin-left: 24pt; width: 12.5vw;" :precision="2" :step="0.01" :min="0" />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                数量：
                <el-input-number v-model="newBookInfo.stock" style="margin-left: 24pt; width: 12.5vw;" :min="1" :step="1" step-strictly />
            </div>

            <template #footer>
                <span>
                    <el-button @click="newBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmNewBook" :disabled="newBookInfo.category.length === 0 || newBookInfo.title.length === 0 || newBookInfo.author.length === 0 || newBookInfo.press.length === 0 || newBookInfo.publishYear == null || newBookInfo.price == null || newBookInfo.stock == null">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 批量入库对话框 -->
        <el-dialog v-model="newBookSetVisible" title="批量添加图书" width="30%" align-center>
            <el-upload style="display: inline" :auto-upload="false" :on-change="handleChange" :file-list="fileList" action="#">
                <el-button style="margin-left: 10pt; margin-top: 10pt" type="success">选择文件</el-button>
                <template #tip>
                    <div style="margin-left: 10pt" class="el-upload__tip">
                        请上传.csv文件
                    </div>
                </template>
            </el-upload>

            <template #footer>
                <span>
                    <el-button @click="newBookSetVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmNewBookSet" :disabled="false">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 借书对话框 -->
        <el-dialog v-model="borrowBookVisible" title="图书借阅" width="35%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                借书证ID：
                <el-input v-model="borrowReturnCard" style="margin-left: 12pt; width: 20vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书ID：
                <el-input v-model="borrowReturnBook" style="margin-left: 24pt; width: 20vw;" clearable />
            </div>

            <template #footer>
                <span>
                    <el-button @click="borrowBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmBorrowBook" :disabled="borrowReturnBook.length === 0 || borrowReturnCard.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 还书对话框 -->
        <el-dialog v-model="returnBookVisible" title="图书归还" width="35%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                借书证ID：
                <el-input v-model="borrowReturnCard" style="margin-left: 12pt; width: 20vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书ID：
                <el-input v-model="borrowReturnBook" style="margin-left: 24pt; width: 20vw;" clearable />
            </div>

            <template #footer>
                <span>
                    <el-button @click="borrowBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmReturnBook" :disabled="borrowReturnBook.length === 0 || borrowReturnCard.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 修改信息对话框 -->
        <el-dialog v-model="modifyBookVisible" :title="'修改信息(图书ID: ' + this.toModifyInfo.book_id + ')'" width="35%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类别：
                <el-input v-model="toModifyInfo.category" style="margin-left: 24pt; width: 20vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                书名：
                <el-input v-model="toModifyInfo.title" style="margin-left: 24pt; width: 20vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                作者：
                <el-input v-model="toModifyInfo.author" style="margin-left: 24pt; width: 20vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社：
                <el-input v-model="toModifyInfo.press" style="margin-left: 12pt; width: 20vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版年份：
                <el-input-number v-model="toModifyInfo.publishYear" style="width: 12.5vw;" :min="-5000" :step="1" step-strictly />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                价格：
                <el-input-number v-model="toModifyInfo.price" style="margin-left: 24pt; width: 12.5vw;" :precision="2" :step="0.01" :min="0" />
            </div>

            <template #footer>
                <span>
                    <el-button @click="modifyBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmModifyBook" :disabled="toModifyInfo.category.length === 0 || toModifyInfo.title.length === 0 || toModifyInfo.author.length === 0 || toModifyInfo.press.length === 0 || toModifyInfo.publishYear == null || toModifyInfo.price == null">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 修改库存对话框 -->
        <el-dialog v-model="modifyStockVisible" title="修改库存" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                库存增量：
                <el-input-number v-model="toModifyInfo.stock" style="width: 12.5vw;"  :step="1" step-strictly />
            </div>

            <template #footer>
                <span>
                    <el-button @click="modifyStockVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmModifyBook" :disabled="toModifyInfo.stock == null">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 删除图书对话框 -->
        <el-dialog v-model="removeBookVisible" title="删除图书" width="30%">
            <span>确定删除<span style="font-weight: bold;">{{ toRemove }}号图书</span>吗？</span>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="removeBookVisible = false">取消</el-button>
                    <el-button type="danger" @click="ConfirmRemoveBook">
                        删除
                    </el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 结果表格 -->
        <el-table v-if="isShow" :data="books" height="450"
            :default-sort="{ prop: 'book_id', order: 'ascending' }" :table-layout="'auto'"
            style="width: 100%; margin-left: 50px; margin-top: 30px; margin-right: 50px; max-width: 80vw;">
            <el-table-column prop="book_id" label="编号" width="90" sortable align="center"/>
            <el-table-column prop="title" label="书名" width="180" sortable align="center"/>
            <el-table-column prop="author" label="作者" width="110" sortable align="center"/>
            <el-table-column prop="category" label="类别" width="150" sortable align="center"/>
            <el-table-column prop="press" label="出版社" width="110" sortable align="center"/>
            <el-table-column prop="publishYear" label="出版年份" width="110" sortable align="center"/>
            <el-table-column prop="price" label="价格" width="80" sortable :formatter="formatPrice" align="center"/>
            <el-table-column prop="stock" label="库存" width="80" sortable align="center"/>
            <el-table-column label="操作" width="250" align="center">
                <template #default="scope">
                    <el-button size="small" type="primary" @click="toModifyInfo.book_id = scope.row.book_id, toModifyInfo.category = scope.row.category, toModifyInfo.title = scope.row.title, toModifyInfo.author = scope.row.author, toModifyInfo.press = scope.row.press, toModifyInfo.publishYear = scope.row.publishYear, toModifyInfo.price = scope.row.price, toModifyInfo.stock = -100000000, modifyBookVisible = true">编辑</el-button>

                    <el-button size="small" type="warning" @click="toModifyInfo.book_id = scope.row.book_id, toModifyInfo.category = scope.row.category, toModifyInfo.title = scope.row.title, toModifyInfo.author = scope.row.author, toModifyInfo.press = scope.row.press, toModifyInfo.publishYear = scope.row.publishYear, toModifyInfo.price = scope.row.price, toModifyInfo.stock = 0, modifyStockVisible = true">修改库存</el-button>

                    <el-button size="small" type="danger" @click="this.toRemove = scope.row.book_id, removeBookVisible = true">删除</el-button>
                </template>
                
            </el-table-column>
        </el-table>
        
    </el-scrollbar>
</template>

<script>
import axios from 'axios';
import { ElMessage } from 'element-plus'

export default {
    data() {
        return {
            isShow: false, // 结果表格展示状态
            newBookVisible: false, // 新建图书对话框可见性
            newBookSetVisible: false, // 批量入库图书对话框可见性
            modifyBookVisible: false, // 编辑图书对话框可见性
            removeBookVisible: false, // 删除图书对话框可见性
            modifyStockVisible: false, // 修改库存对话框可见性
            borrowBookVisible: false, // 借书对话框可见性
            returnBookVisible: false, // 还书对话框可见性
            toRemove: 0, // 待删除的图书编号
            borrowReturnBook: 0, // 借/还的图书编号
            borrowReturnCard: 0, // 借/还的借书证编号
            selectedFileList: [],
            books: [{ // 书籍列表
                book_id: 0,
                category: 'test',
                title: 'test',
                press: 'test',
                minPublishYear: 0,
                maxPublishYear: 0,
                author: 'test',
                minPrice: 0,
                maxPrice: 0,
                stock: 0
            }],
            newBookInfo: { // 待新建图书信息
                category: '',
                title: '',
                author: '',
                press: '',
                publishYear: 0,
                price: 0.0,
                stock: 0
            },
            toModifyInfo: { // 待修改图书信息
                card_id: 0,
                book_id: 0,
                category: '',
                title: '',
                author: '',
                press: '',
                publishYear: 0,
                price: 0.0,
                stock: 0
            },
            toQueryInfo: { // 待查询图书信息
                category: '',
                title: '',
                press: '',
                minPublishYear: '',
                maxPublishYear: '',
                author: '',
                minPrice: '',
                maxPrice: ''
            },
        }
    },
    methods: {
        handleChange(file, fileList) {
            if (fileList.length > 1) { // 只能选择一个文件
                fileList.splice(0, fileList.length - 1);
            }
            this.selectedFileList = fileList;
        },
        ConfirmNewBookSet() {
            if (this.selectedFileList.length == 0) {
                ElMessage.error("请选择文件");
                return;
            }
            const file = this.selectedFileList[0];
            const fileType = file.name.split('.').pop();
            if (fileType != 'csv') {
                ElMessage.error("请上传.csv文件");
                return;
            }
            const formData = new FormData();
            formData.append('file', file.raw);
            
            axios.post('/bookset', formData, {
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                })
                .then(response => {
                    ElMessage.success(response.data)
                    this.newBookSetVisible = false
                    this.QueryBooks()
                })
                .catch(error => {
                    ElMessage.error(error.response.data)
                })
            
        },
        ConfirmNewBook() {
            axios.post("/book",
                {
                    category: this.newBookInfo.category,
                    title: this.newBookInfo.title,
                    author: this.newBookInfo.author,
                    press: this.newBookInfo.press,
                    publishYear: this.newBookInfo.publishYear,
                    price: this.newBookInfo.price,
                    stock: this.newBookInfo.stock,
                })
                .then(response => {
                    ElMessage.success(response.data)
                    this.newBookVisible = false
                    this.QueryBooks()
                })
                .catch(error => {
                    ElMessage.error(error.response.data)
                })
            
        },
        ConfirmModifyBook() {
            axios.put("/book",
                {
                    book_id: this.toModifyInfo.book_id,
                    category: this.toModifyInfo.category,
                    title: this.toModifyInfo.title,
                    press: this.toModifyInfo.press,
                    author: this.toModifyInfo.author,
                    price: this.toModifyInfo.price,
                    publishYear: this.toModifyInfo.publishYear,
                    stock: this.toModifyInfo.stock, // 库存增量，编辑图书信息时设置为-100000000
                })
                .then(response => {
                    ElMessage.success(response.data)
                    this.modifyBookVisible = false
                    this.modifyStockVisible = false
                    this.QueryBooks()
                })
                .catch(error => {
                    ElMessage.error(error.response.data)
                })
        },
        ConfirmRemoveBook() {
            axios.delete("/book",
                {
                    data: {
                        id: this.toRemove
                    }
                })
                .then(response => {
                    ElMessage.success(response.data)
                    this.removeBookVisible = false
                    this.QueryBooks()
                })
                .catch(error => {
                    ElMessage.error(error.response.data)
                })
        },
        ConfirmBorrowBook() {
            axios.put("/borrow",
                {
                    book_id: this.borrowReturnBook,
                    card_id: this.borrowReturnCard
                })
                .then(response => {
                    ElMessage.success(response.data)
                    this.borrowBookVisible = false
                    this.QueryBooks()
                })
                .catch(error => {
                    ElMessage.error(error.response.data)
                })
        },
        ConfirmReturnBook() {
            axios.put("/return",
                {
                    book_id: this.borrowReturnBook,
                    card_id: this.borrowReturnCard
                })
                .then(response => {
                    ElMessage.success(response.data)
                    this.returnBookVisible = false
                    this.QueryBooks()
                })
                .catch(error => {
                    ElMessage.error(error.response.data)
                })
        },
        async QueryBooks() {
            this.books = [] // 清空列表
            let response = await axios.get('/book',
                {
                    params: {
                        category: this.toQueryInfo.category,
                        title: this.toQueryInfo.title,
                        press: this.toQueryInfo.press,
                        minPublishYear: this.toQueryInfo.minPublishYear,
                        maxPublishYear: this.toQueryInfo.maxPublishYear,
                        author: this.toQueryInfo.author,
                        minPrice: this.toQueryInfo.minPrice,
                        maxPrice: this.toQueryInfo.maxPrice
                    }
                })
            let books = response.data
            books.forEach(book => {
                this.books.push(book)
            });
            this.isShow = true
        },
        formatPrice(row, column, cellValue) {
            if (cellValue === null || cellValue === undefined) {
                return '';
            }
            return parseFloat(cellValue).toFixed(2);
        }
    },
    mounted() {
        this.QueryBooks()
    }
}
</script>